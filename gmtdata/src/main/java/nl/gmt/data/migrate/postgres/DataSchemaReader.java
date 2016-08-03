package nl.gmt.data.migrate.postgres;

import com.google.gson.Gson;
import nl.gmt.data.migrate.*;
import nl.gmt.data.schema.SchemaDbType;
import nl.gmt.data.schema.SchemaIndexType;
import org.apache.commons.lang.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class DataSchemaReader extends nl.gmt.data.migrate.DataSchemaReader {
    private static final Gson GSON = new Gson();

    public DataSchemaReader(Connection connection) {
        super(connection);
    }

    @Override
    public Map<String, DataSchemaTable> getTables() throws SchemaMigrateException {
        try {
            Map<Tuple, FullType> columnTypes = new HashMap<>();

            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(
                    "select t.relname, a.attname, a.attndims, ty.typname\n" +
                    "from pg_attribute a left join pg_class t on a.attrelid = t.oid left join pg_type ty on a.atttypid = ty.oid\n" +
                    "where t.relkind = 'r' and a.attisdropped = false and attnum > 0 and t.relnamespace = (select n.oid from pg_namespace n where n.nspname = current_schema())"
                )
            ) {
                while (rs.next()) {
                    String typeName = rs.getString("typname");
                    if (typeName.startsWith("_")) {
                        typeName = typeName.substring(1);
                    }

                    columnTypes.put(
                        new Tuple(rs.getString("relname"), rs.getString("attname")),
                        new FullType(parseType(typeName), rs.getInt("attndims"))
                    );
                }
            }

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

                    FullType type = columnTypes.get(new Tuple(rs.getString("TABLE_NAME"), field.getName()));

                    field.setType(type.type);
                    field.setArity(type.arity);

                    String columnDefault = rs.getString("COLUMN_DEFAULT");
                    // I can't find out how to detect whether a sequence is associated with a column.
                    field.setAutoIncrement(columnDefault != null && columnDefault.toLowerCase().startsWith("nextval("));

                    tables.get(rs.getString("TABLE_NAME")).addField(field);
                }
            }

            Map<Tuple, Index> indexesMap = new HashMap<>();

            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(
                    "select t.relname as \"trelname\", i.relname as \"irelname\", ix.indnatts, ix.indisunique, ix.indisprimary, ix.indkey, a.amname, obj_description(ix.indexrelid) as \"incomment\"\n" +
                    "from pg_index ix left join pg_class t on ix.indrelid = t.oid left join pg_class i on ix.indexrelid = i.oid left join pg_am a on i.relam = a.oid\n" +
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

                    index.setStrategy(rs.getString("amname"));
                    IndexComment comment = parseComment(rs.getString("incomment"));
                    index.setFilter(comment.getFilter());

                    indexesMap.put(new Tuple(rs.getString("trelname"), index.getName()), new Index(index, parseAttributes(rs.getObject("indkey").toString())));
                    tables.get(rs.getString("trelname")).getIndexes().add(index);
                }
            }

            Map<Tuple, Map<Integer, String>> indexesFields = new HashMap<>();

            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(
                    "select i.relname as \"irelname\", t.relname as \"trelname\", a.attname, a.attnum\n" +
                    "from pg_index ix left join pg_class t on ix.indrelid = t.oid left join pg_class i on ix.indexrelid = i.oid left join pg_attribute a on a.attrelid = t.oid\n" +
                    "where a.attnum = ANY(ix.indkey) and t.relkind = 'r' and a.attisdropped = false and t.relnamespace = (select n.oid from pg_namespace n where n.nspname = current_schema())\n" +
                    "order by t.relname, i.relname"
                )
            ) {
                while (rs.next()) {
                    String indexName = rs.getString("irelname");
                    String columnName = rs.getString("attname");
                    int columnNumber = rs.getInt("attnum");

                    Tuple key = new Tuple(rs.getString("trelname"), indexName);

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

            for (Map.Entry<Tuple, Map<Integer, String>> entry : indexesFields.entrySet()) {
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

    private IndexComment parseComment(String comment) {
        if (StringUtils.isEmpty(comment)) {
            return new IndexComment();
        }

        return GSON.fromJson(comment, IndexComment.class);
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

            case "CITEXT":
                return SchemaDbType.CASE_INSENSITIVE_TEXT;

            default:
                throw new SchemaMigrateException(String.format("Unknown data type '%s'", dataType));
        }
    }

    private static class Tuple {
        final String item1;
        final String item2;

        private Tuple(String item1, String item2) {
            this.item1 = item1.toUpperCase();
            this.item2 = item2.toUpperCase();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Tuple)) {
                return false;
            }

            Tuple other = (Tuple)obj;

            return item2.equals(other.item2) && item1.equals(other.item1);
        }

        @Override
        public int hashCode() {
            return (item1.hashCode() * 31) + item2.hashCode();
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

    private static class FullType {
        final SchemaDbType type;
        final int arity;

        public FullType(SchemaDbType type, int arity) {
            this.type = type;
            this.arity = arity;
        }
    }
}
