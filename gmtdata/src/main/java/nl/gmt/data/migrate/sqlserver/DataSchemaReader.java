package nl.gmt.data.migrate.sqlserver;

import nl.gmt.data.migrate.*;
import nl.gmt.data.schema.SchemaDbType;
import nl.gmt.data.schema.SchemaIndexType;
import org.apache.commons.lang.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
public class DataSchemaReader extends nl.gmt.data.migrate.DataSchemaReader {
    public DataSchemaReader(Connection connection) {
        super(connection);
    }

    private boolean ignoreTable(String tableName) {
        switch (tableName.toUpperCase()) {
            case "DTPROPERTIES":
            case "SYSDIAGRAMS":
            case "GMTDATASCHEMA":
                return true;
            default:
                return false;
        }
    }

    @Override
    public Map<String, DataSchemaTable> getTables() throws SchemaMigrateException {
        try {
            boolean haveSchemaTable = false;

            Map<String, DataSchemaTable> tables = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery("SELECT [name] FROM [sys].[tables]")
            ) {
                while (rs.next()) {
                    String tableName = rs.getString(1);

                    if (StringUtils.equalsIgnoreCase(tableName, "GMTDATASCHEMA")) {
                        haveSchemaTable = true;
                    }

                    if (!ignoreTable(tableName)) {
                        DataSchemaTable table = new DataSchemaTable();

                        table.setName(tableName);

                        tables.put(table.getName(), table);
                    }
                }
            }

            Map<String, String> settings = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            if (haveSchemaTable) {
                try (
                    Statement stmt = getConnection().createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT [key], [value] FROM [GMTDATASCHEMA]")
                ) {
                    while (rs.next()) {
                        settings.put(rs.getString(1), rs.getString(2));
                    }
                }
            }

            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(
                    "SELECT [tl].[name], [c].[name], [c].[max_length], [c].[precision], [c].[scale], [c].[is_nullable], [t].[name], [c].[is_identity] " +
                    "FROM [sys].[columns] [c] " +
                    "LEFT JOIN [sys].[types] [t] ON [c].[user_type_id] = [t].[user_type_id] " +
                    "LEFT JOIN [sys].[tables] [tl] ON [c].[object_id] = [tl].[object_id] " +
                    "WHERE [tl].[name] IS NOT NULL"
                )
            ) {
                while (rs.next()) {
                    DataSchemaField field = new DataSchemaField();

                    String tableName = rs.getString(1);
                    if (ignoreTable(tableName)) {
                        continue;
                    }

                    field.setName(rs.getString(2));
                    int length = rs.getInt(3);
                    int precision = rs.getInt(4);
                    int scale = rs.getInt(5);
                    field.setNullable(rs.getBoolean(6));
                    String dataTypeName = rs.getString(7);
                    field.setType(parseType(dataTypeName));
                    field.setAutoIncrement(rs.getBoolean(8));

                    field.setLength(-1);
                    field.setPositions(-1);

                    if (precision > 0) {
                        field.setLength(precision);
                        field.setPositions(scale);
                    } else if (length > 0) {
                        switch (dataTypeName.toUpperCase()) {
                            case "NCHAR":
                            case "NVARCHAR":
                                length /= 2;
                        }

                        field.setLength(length);
                    }

                    tables.get(tableName).addField(field);
                }
            }

            Map<String, DataSchemaIndex> indexesMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(
                    "SELECT [i].[name], [i].[type], [i].[is_unique], [i].[is_primary_key], [i].[has_filter], [tl].[name] " +
                    "FROM [sys].[indexes] [i] " +
                    "LEFT JOIN [sys].[tables] [tl] ON [i].[object_id] = [tl].[object_id] " +
                    "WHERE [is_hypothetical] = 0 AND [i].[name] IS NOT NULL AND [tl].[name] IS NOT NULL"
                )
            ) {
                while (rs.next()) {
                    String tableName = rs.getString(6);
                    if (ignoreTable(tableName)) {
                        continue;
                    }
                    String indexName = rs.getString(1);
                    int type = rs.getInt(2);
                    boolean isUnique = rs.getBoolean(3);
                    boolean isPrimary = rs.getBoolean(4);
                    boolean hasFilter = rs.getBoolean(5);

                    String filter = settings.get(tableName + "::" + indexName + "::filter");

                    if (!hasFilter) {
                        filter = null;
                    }

                    SchemaIndexType indexType;
                    if (isPrimary) {
                        indexType = SchemaIndexType.PRIMARY;
                    } else if (isUnique) {
                        indexType = SchemaIndexType.UNIQUE;
                    } else {
                        indexType = SchemaIndexType.INDEX;
                    }

                    String strategy = null;
                    switch (type) {
                        case 1:
                            strategy = "CLUSTERED";
                            break;
                        case 2:
                            strategy = "NONCLUSTERED";
                            break;
                    }

                    DataSchemaIndex index = new DataSchemaIndex();
                    index.setName(indexName);
                    index.setType(indexType);
                    index.setStrategy(strategy);
                    index.setFilter(filter);

                    indexesMap.put(indexName, index);
                    tables.get(tableName).getIndexes().add(index);
                }
            }

            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(
                    "SELECT [i].[name], col_name([c].[object_id], [c].[column_id]), [c].[is_included_column] " +
                    "FROM [sys].[index_columns] [c] " +
                    "LEFT JOIN [sys].[indexes] [i] ON [c].[index_id] = [i].[index_id] AND [c].[object_id] = [i].[object_id] " +
                    "ORDER BY [i].[name], [c].[key_ordinal]"
                )
            ) {
                while (rs.next()) {
                    String indexName = rs.getString(1);
                    DataSchemaIndex index = indexesMap.get(indexName);
                    if (index != null) {
                        index.getFields().add(rs.getString(2));
                    }
                }
            }

            Map<String, DataSchemaForeignKey> foreignKeys = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(
                    "SELECT [name], object_name([parent_object_id]), object_name([referenced_object_id]) " +
                    "FROM [sys].[foreign_keys]"
                )
            ) {
                while (rs.next()) {
                    String tableName = rs.getString(2);
                    if (!ignoreTable(tableName)) {
                        DataSchemaForeignKey foreignKey = new DataSchemaForeignKey();

                        foreignKey.setName(rs.getString(1));
                        foreignKey.setLinkTable(rs.getString(3));

                        tables.get(tableName).getForeignKeys().add(foreignKey);
                        foreignKeys.put(foreignKey.getName(), foreignKey);
                    }
                }
            }

            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(
                    "SELECT [f].[name], col_name([fc].[parent_object_id], [fc].[parent_column_id]), col_name([fc].[referenced_object_id], [fc].[referenced_column_id]) " +
                    "FROM [sys].[foreign_key_columns] [fc] " +
                    "LEFT JOIN [sys].[foreign_keys] [f] ON [fc].[constraint_object_id] = [f].[object_id]"
                )
            ) {
                while (rs.next()) {
                    DataSchemaForeignKey foreignKey = foreignKeys.get(rs.getString(1));
                    if (foreignKey != null) {
                        if (foreignKey.getField() != null) {
                            throw new SchemaMigrateException("Composite foreign keys are not supported");
                        }

                        foreignKey.setField(rs.getString(2));
                        foreignKey.setLinkField(rs.getString(3));
                    }
                }
            }


            return tables;
        } catch (SQLException e) {
            throw new SchemaMigrateException("Cannot load schema", e);
        }
    }

    private SchemaDbType parseType(String dataType) throws SchemaMigrateException {
        switch (dataType.toUpperCase()) {
            case "BINARY": return SchemaDbType.FIXED_BINARY;
            case "DATETIME2": return SchemaDbType.DATE_TIME;
            case "DECIMAL": return SchemaDbType.DECIMAL;
            case "INT": return SchemaDbType.INT;
            case "NCHAR": return SchemaDbType.FIXED_STRING;
            case "NTEXT": return SchemaDbType.TEXT;
            case "NVARCHAR": return SchemaDbType.STRING;
            case "REAL": return SchemaDbType.DOUBLE;
            case "SMALLINT": return SchemaDbType.SMALL_INT;
            case "TINYINT": return SchemaDbType.TINY_INT;
            case "UNIQUEIDENTIFIER": return SchemaDbType.GUID;
            case "VARBINARY": return SchemaDbType.BINARY;
            case "IMAGE": return SchemaDbType.BLOB;

            default:
                throw new SchemaMigrateException(String.format("Unknown data type '%s'", dataType));
        }
    }
}
