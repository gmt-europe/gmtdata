package nl.gmt.data.migrate.postgres;

import nl.gmt.data.migrate.*;
import nl.gmt.data.schema.SchemaDbType;
import nl.gmt.data.schema.SchemaIndexType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class DataSchemaReader extends nl.gmt.data.migrate.DataSchemaReader {
    public DataSchemaReader(Connection connection) {
        super(connection);
    }

    @Override
    public Map<String, DataSchemaTable> getTables() throws SchemaMigrateException {
        try {
            Map<String, DataSchemaTable> tables = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = current_schema()")
            ) {
                while (rs.next()) {
                    DataSchemaTable table = new DataSchemaTable();

                    table.setName(rs.getString("TABLE_NAME"));

                    tables.put(table.getName(), table);
                }
            }

            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = current_schema()")
            ) {
                while (rs.next()) {
                    DataSchemaField field = new DataSchemaField();

                    field.setName(rs.getString("COLUMN_NAME"));

                    // We check CHARACTER_MAXIMUM_LENGTH because TEXT fields return unsigned integer max value (4G)
                    // which we can't store in an integer, and we don't care about because it's a TEXT field.

                    if (rs.getObject("NUMERIC_PRECISION") != null) {
                        field.setLength(rs.getInt("NUMERIC_PRECISION"));
                    } else if (
                        rs.getObject("CHARACTER_MAXIMUM_LENGTH") != null &&
                            rs.getLong("CHARACTER_MAXIMUM_LENGTH") < Integer.MAX_VALUE
                        ) {
                        field.setLength(rs.getInt("CHARACTER_MAXIMUM_LENGTH"));
                    } else {
                        field.setLength(-1);
                    }

                    if (rs.getObject("NUMERIC_SCALE") != null) {
                        field.setPositions(rs.getInt("NUMERIC_SCALE"));
                    } else {
                        field.setPositions(-1);
                    }

                    field.setNullable(rs.getString("IS_NULLABLE").equals("YES"));
                    field.setType(parseType(rs.getString("DATA_TYPE")));

                    String columnDefault = rs.getString("COLUMN_DEFAULT");
                    // I can't find out how to detect whether a sequence is associated with a column.
                    field.setAutoIncrement(columnDefault != null && columnDefault.toLowerCase().startsWith("nextval("));

                    tables.get(rs.getString("TABLE_NAME")).addField(field);
                }
            }

            Map<IndexKey, Index> indexesMap = new HashMap<>();

            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(
                    "select t.relname as \"trelname\", i.relname as \"irelname\", ix.indnatts, ix.indisunique, ix.indisprimary, ix.indkey\n" +
                    "from pg_index ix left join pg_class t on ix.indrelid = t.oid left join pg_class i on ix.indexrelid = i.oid\n" +
                    "where t.relkind = 'r' and t.relnamespace = (select n.oid from pg_namespace n where n.nspname = current_schema())\n" +
                    "order by t.relname, i.relname"
                )
            ) {
                while (rs.next()) {
                    DataSchemaIndex index = new DataSchemaIndex();
                    index.setName(rs.getString("irelname"));
                    if (rs.getBoolean("indisprimary")) {
                        index.setType(SchemaIndexType.PRIMARY);
                    } else if (rs.getBoolean("indisunique")) {
                        index.setType(SchemaIndexType.UNIQUE);
                    } else {
                        index.setType(SchemaIndexType.INDEX);
                    }

                    indexesMap.put(new IndexKey(rs.getString("trelname"), index.getName()), new Index(index, parseAttributes(rs.getObject("indkey").toString())));
                    tables.get(rs.getString("trelname")).getIndexes().add(index);
                }
            }

            Map<IndexKey, Map<Integer, String>> indexesFields = new HashMap<>();

            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(
                    "select i.relname as \"irelname\", t.relname as \"trelname\", a.attname, a.attnum\n" +
                    "from pg_index ix left join pg_class t on ix.indrelid = t.oid left join pg_class i on ix.indexrelid = i.oid left join pg_attribute a on a.attrelid = t.oid\n" +
                    "where a.attnum = ANY(ix.indkey) and t.relkind = 'r' and t.relnamespace = (select n.oid from pg_namespace n where n.nspname = current_schema())\n" +
                    "order by t.relname, i.relname"
                )
            ) {
                while (rs.next()) {
                    String indexName = rs.getString("irelname");
                    String columnName = rs.getString("attname");
                    int columnNumber = rs.getInt("attnum");

                    IndexKey key = new IndexKey(rs.getString("trelname"), indexName);

                    int columnIndex = indexesMap.get(key).attributes.indexOf(columnNumber);
                    if (columnIndex == -1) {
                        throw new IllegalStateException("Expected attribute number to be in the index");
                    }

                    Map<Integer, String> columns = indexesFields.get(key);
                    if (columns == null) {
                        columns = new HashMap<>();
                        indexesFields.put(key, columns);
                    }

                    columns.put(columnIndex, columnName);
                }
            }

            for (Map.Entry<IndexKey, Map<Integer, String>> entry : indexesFields.entrySet()) {
                List<Map.Entry<Integer, String>> fields = new ArrayList<>(entry.getValue().entrySet());

                Collections.sort(fields, new Comparator<Map.Entry<Integer, String>>() {
                    @Override
                    public int compare(Map.Entry<Integer, String> a, Map.Entry<Integer, String> b) {
                        return a.getKey().compareTo(b.getKey());
                    }
                });

                List<String> indexFields = indexesMap.get(entry.getKey()).index.getFields();

                for (Map.Entry<Integer, String> field : fields) {
                    indexFields.add(field.getValue());
                }
            }

            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(
                    "SELECT tc.table_name, tc.constraint_name, kcu.column_name, ccu.table_name AS foreign_table_name, ccu.column_name AS foreign_column_name\n" +
                    "FROM information_schema.table_constraints AS tc JOIN information_schema.key_column_usage AS kcu ON tc.constraint_name = kcu.constraint_name JOIN information_schema.constraint_column_usage AS ccu ON ccu.constraint_name = tc.constraint_name\n" +
                    "WHERE tc.constraint_type = 'FOREIGN KEY' and tc.table_schema = current_schema()\n" +
                    "ORDER BY tc.constraint_name"
                )
            ) {
                String lastConstraintName = null;
                while (rs.next()) {
                    String constraintName = rs.getString("constraint_name");
                    if (constraintName.equals(lastConstraintName)) {
                        throw new SchemaMigrateException("Composite foreign keys are not supported");
                    }
                    lastConstraintName = constraintName;

                    DataSchemaForeignKey foreignKey = new DataSchemaForeignKey();

                    foreignKey.setName(constraintName);
                    foreignKey.setField(rs.getString("column_name"));
                    foreignKey.setLinkTable(rs.getString("foreign_table_name"));
                    foreignKey.setLinkField(rs.getString("foreign_column_name"));

                    tables.get(rs.getString("table_name")).getForeignKeys().add(foreignKey);
                }
            }

            return tables;
        } catch (SQLException e) {
            throw new SchemaMigrateException("Cannot load schema", e);
        }
    }

    private List<Integer> parseAttributes(String attributes) {
        List<Integer> result = new ArrayList<>();

        for (String part : attributes.split(" ")) {
            result.add(Integer.parseInt(part));
        }

        return result;
    }

    private SchemaDbType parseType(String dataType) throws SchemaMigrateException {
        switch (dataType.toUpperCase()) {
            case "JSONB":
                return SchemaDbType.BINARY_JSON;

            case "BYTEA":
                return SchemaDbType.BLOB;

            case "TIMESTAMP":
            case "TIMESTAMP WITHOUT TIME ZONE":
                return SchemaDbType.DATE_TIME;

            case "DECIMAL":
            case "NUMERIC":
                return SchemaDbType.DECIMAL;

            case "DOUBLE PRECISION":
            case "FLOAT8":
                return SchemaDbType.DOUBLE;

            case "CHAR":
            case "CHARACTER":
                return SchemaDbType.FIXED_STRING;

            case "UUID":
                return SchemaDbType.GUID;

            case "INT":
            case "INT4":
            case "INTEGER":
                return SchemaDbType.INT;

            case "JSON":
                return SchemaDbType.JSON;

            case "INT2":
            case "SMALLINT":
                return SchemaDbType.SMALL_INT;

            case "VARCHAR":
            case "CHARACTER VARYING":
                return SchemaDbType.STRING;

            case "TEXT":
                return SchemaDbType.TEXT;

            case "BOOL":
            case "BOOLEAN":
                return SchemaDbType.TINY_INT;

            default:
                throw new SchemaMigrateException(String.format("Unknown data type '%s'", dataType));
        }
    }

    private static class IndexKey {
        final String table;
        final String name;

        private IndexKey(String table, String name) {
            this.table = table.toUpperCase();
            this.name = name.toUpperCase();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof IndexKey)) {
                return false;
            }

            IndexKey other = (IndexKey)obj;

            return name.equals(other.name) && table.equals(other.table);
        }

        @Override
        public int hashCode() {
            return (table.hashCode() * 31) + name.hashCode();
        }
    }

    private static class Index {
        final DataSchemaIndex index;
        final List<Integer> attributes;

        public Index(DataSchemaIndex index, List<Integer> attributes) {
            this.index = index;
            this.attributes = attributes;
        }
    }
}
