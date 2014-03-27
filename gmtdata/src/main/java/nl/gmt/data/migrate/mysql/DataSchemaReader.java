package nl.gmt.data.migrate.mysql;

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
    private final Map<String, String> defaultCharacterSets;

    public DataSchemaReader(Connection connection) throws SchemaMigrateException {
        super(connection);

        try {
            defaultCharacterSets = loadDefaultCharacterSets();
        } catch (SQLException e) {
            throw new SchemaMigrateException("Cannot get collations", e);
        }
    }

    private Map<String, String> loadDefaultCharacterSets() throws SQLException {
        Map<String, String> defaultCharacterSets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        try (
            Statement stmt = getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("SHOW COLLATION")
        ) {
            while (rs.next()) {
                defaultCharacterSets.put(rs.getString(1), rs.getString(2));
            }
        }

        return defaultCharacterSets;
    }

    @Override
    public Map<String, DataSchemaTable> getTables() throws SchemaMigrateException {
        try {
            Map<String, DataSchemaTable> tables = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = schema()")
            ) {
                while (rs.next()) {
                    DataSchemaTable table = createTable(rs);
                    tables.put(table.getName(), table);
                }
            }

            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = schema()")
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
                    
                    field.setNullable(rs.getBoolean("IS_NULLABLE"));
                    field.setType(parseType(rs.getString("DATA_TYPE")));
                    field.setAutoIncrement(StringUtils.equalsIgnoreCase(rs.getString("EXTRA"), "auto_increment"));

                    String collation = rs.getString("COLLATION_NAME");

                    if (SchemaRules.dbTypeSupportsCharset(field.getType())) {
                        if (collation == null) {
                            SchemaRules.setCollation(field, SchemaRules.DEFAULT_COLLATION);
                            SchemaRules.setCharset(field, SchemaRules.DEFAULT_CHARSET);
                        } else {
                            SchemaRules.setCollation(field, collation);
                            SchemaRules.setCharset(field, defaultCharacterSets.get(collation));
                        }
                    }

                    tables.get(rs.getString("TABLE_NAME")).addField(field);
                }
            }

            Map<IndexKey, DataSchemaIndex> indexesMap = new HashMap<>();

            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = schema() AND SEQ_IN_INDEX = 1")
            ) {
                while (rs.next()) {
                    DataSchemaIndex index = new DataSchemaIndex();

                    String indexName = rs.getString("INDEX_NAME");
                    index.setName(indexName);
                    index.setType(StringUtils.equalsIgnoreCase(indexName, "PRIMARY")
                        ? SchemaIndexType.PRIMARY
                        : rs.getBoolean("NON_UNIQUE") ? SchemaIndexType.INDEX : SchemaIndexType.UNIQUE
                    );

                    indexesMap.put(new IndexKey(rs.getString("TABLE_NAME"), index.getName()), index);
                    tables.get(rs.getString("TABLE_NAME")).getIndexes().add(index);
                }
            }

            Map<IndexKey, Map<Integer, String>> indexesFields = new HashMap<>();

            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = schema()")
            ) {
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    String columnName = rs.getString("COLUMN_NAME");
                    int columnIndex = rs.getInt("SEQ_IN_INDEX");

                    IndexKey key = new IndexKey(rs.getString("TABLE_NAME"), indexName);

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

                List<String> indexFields = indexesMap.get(entry.getKey()).getFields();

                for (Map.Entry<Integer, String> field : fields) {
                    indexFields.add(field.getValue());
                }
            }

            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = schema() AND REFERENCED_TABLE_NAME IS NOT NULL")
            ) {
                while (rs.next()) {
                    if (rs.getInt("ORDINAL_POSITION") != 1) {
                        throw new SchemaMigrateException("Composite foreign keys are not supported");
                    }

                    DataSchemaForeignKey foreignKey = new DataSchemaForeignKey();

                    foreignKey.setName(rs.getString("CONSTRAINT_NAME"));
                    foreignKey.setField(rs.getString("COLUMN_NAME"));
                    foreignKey.setLinkTable(rs.getString("REFERENCED_TABLE_NAME"));
                    foreignKey.setLinkField(rs.getString("REFERENCED_COLUMN_NAME"));

                    tables.get(rs.getString("TABLE_NAME")).getForeignKeys().add(foreignKey);
                }
            }

            return tables;
        } catch (SQLException e) {
            throw new SchemaMigrateException("Cannot load schema", e);
        }
    }

    private SchemaDbType parseType(String dataType) throws SchemaMigrateException {
        switch (dataType.toLowerCase()) {
            case "int":
            case "integer":
                return SchemaDbType.INT;

            case "double":
            case "real":
                return SchemaDbType.DOUBLE;

            case "decimal":
            case "dec":
            case "numeric":
                return SchemaDbType.DECIMAL;

            case "bool":
            case "boolean":
            case "tinyint":
                return SchemaDbType.TINY_INT;

            case "smallint": return SchemaDbType.SMALL_INT;
            case "mediumint": return SchemaDbType.MEDIUM_INT;
            case "bigint": return SchemaDbType.BIG_INT;
            case "float": return SchemaDbType.FLOAT;
            case "varchar": return SchemaDbType.STRING;
            case "text": return SchemaDbType.TEXT;
            case "blob": return SchemaDbType.BLOB;
            case "datetime": return SchemaDbType.DATE_TIME;
            case "date": return SchemaDbType.DATE;
            case "timestamp": return SchemaDbType.TIMESTAMP;
            case "time": return SchemaDbType.TIME;
            case "year": return SchemaDbType.YEAR;
            case "char": return SchemaDbType.FIXED_STRING;
            case "binary": return SchemaDbType.FIXED_BINARY;
            case "varbinary": return SchemaDbType.BINARY;
            case "tinyblob": return SchemaDbType.TINY_BLOB;
            case "tinytext": return SchemaDbType.TINY_TEXT;
            case "mediumblob": return SchemaDbType.MEDIUM_BLOB;
            case "mediumtext": return SchemaDbType.MEDIUM_TEXT;
            case "longblob": return SchemaDbType.LONG_BLOB;
            case "longtext": return SchemaDbType.LONG_TEXT;
            case "enum": return SchemaDbType.ENUMERATION;
            case "guid": return SchemaDbType.GUID;

            default:
                throw new SchemaMigrateException(String.format("Unexpected data type '%s'", dataType));
        }
    }

    private DataSchemaTable createTable(ResultSet rs) throws SQLException {
        DataSchemaTable result = new DataSchemaTable();

        result.setName(rs.getString("TABLE_NAME"));
        SchemaRules.setEngine(result, rs.getString("ENGINE"));
        String collation = rs.getString("TABLE_COLLATION");
        SchemaRules.setDefaultCollation(result, collation);
        SchemaRules.setDefaultCharset(result, collation != null ? defaultCharacterSets.get(collation) : null);

        return result;
    }

    private class IndexKey {
        private final String table;
        private final String name;

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
}
