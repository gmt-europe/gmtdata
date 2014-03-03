package nl.gmt.data.migrate.sqlite;

import nl.gmt.data.migrate.*;
import nl.gmt.data.schema.SchemaDbType;
import nl.gmt.data.schema.SchemaIndexType;
import org.apache.commons.lang.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataSchemaReader extends nl.gmt.data.migrate.DataSchemaReader {
    protected DataSchemaReader(Connection connection) {
        super(connection);
    }

    @Override
    public Map<String, DataSchemaTable> getTables() throws SchemaMigrateException {
        try {
            Map<String, DataSchemaTable> tables = new HashMap<>();

            // Load the tables.

            List<String> tableNames = new ArrayList<>();

            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery("select \"name\" from \"sqlite_master\" where \"type\" = 'table'")
            ) {
                while (rs.next()) {
                    tableNames.add(rs.getString("name"));
                }
            }

            for (String tableName : tableNames) {
                tables.put(tableName, loadTable(tableName));
            }

            return tables;
        } catch (SQLException e) {
            throw new SchemaMigrateException("Cannot load schema", e);
        }
    }

    private DataSchemaTable loadTable(String tableName) throws SQLException, SchemaMigrateException {
        DataSchemaTable table = new DataSchemaTable();

        table.setName(tableName);

        // Load the fields.

        List<String> primaryKeyFields = new ArrayList<>();

        try (
            Statement stmt = getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("pragma table_info(" + escape(tableName) + ")")
        ) {
            while (rs.next()) {
                DataSchemaField field = new DataSchemaField();

                field.setName(rs.getString("name"));
                field.setNullable(!rs.getBoolean("notnull"));
                String defaultValue = rs.getString("dflt_value");
                field.setDefaultValue(defaultValue == null ? StringUtils.EMPTY : defaultValue);
                String type = rs.getString("type");
                field.setType(parseType(type));

                // SQLite ignores length specifications, so we do not treat them
                // as part of the schema.

                field.setLength(-1);
                field.setPositions(-1);

                boolean isPrimary = rs.getBoolean("pk");

                if (isPrimary) {
                    primaryKeyFields.add(field.getName());

                    switch (field.getType()) {
                        case INT:
                        case SMALL_INT:
                        case BIG_INT:
                        case TINY_INT:
                            field.setAutoIncrement(true);
                            break;
                    }
                }

                table.addField(field);
            }
        }

        // Create the primary key.

        if (primaryKeyFields.size() == 1) {
            // And create a primary key index.

            DataSchemaIndex index = new DataSchemaIndex();

            index.setType(SchemaIndexType.PRIMARY);
            index.setName(String.format("%s_PK_%s", "sqlite_master", tableName));
            index.getFields().add(primaryKeyFields.get(0));

            table.getIndexes().add(index);

            primaryKeyFields.clear();
        }

        // Load the non-primary indexes. We keep these in a separate list because we need to add the fields
        // below. We don't do this for the primary key we just created, if we created one.

        List<DataSchemaIndex> indexes = new ArrayList<>();

        try (
            Statement stmt = getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("pragma index_list(" + escape(tableName) + ")")
        ) {
            while (rs.next()) {
                DataSchemaIndex index = new DataSchemaIndex();

                index.setName(rs.getString("name"));
                index.setType(rs.getBoolean("unique") ? SchemaIndexType.UNIQUE : SchemaIndexType.INDEX);

                indexes.add(index);
            }
        }

        // Attach the fields to the indexes.

        for (DataSchemaIndex index : indexes) {
            try (
                Statement stmt = getConnection().createStatement();
                ResultSet rs = stmt.executeQuery("pragma index_info(" + escape(index.getName()) + ")")
            ) {
                int nextSequence = 0;

                while (rs.next()) {
                    // Check that the seqno are sorted.
                    if (nextSequence != rs.getInt("seqno"))
                        throw new SchemaMigrateException("Invalid seqno when reading index_info");
                    nextSequence++;

                    index.getFields().add(rs.getString("name"));
                }
            }

            boolean addIndex = true;

            // Check whether this is the primary key index.

            if (StringUtils.startsWithIgnoreCase(index.getName(), "sqlite_autoindex_")) {
                if (
                    primaryKeyFields.size() > 0 &&
                    primaryKeyFields.size() == index.getFields().size()
                ) {
                    boolean matches = true;

                    for (int i = 0; i < primaryKeyFields.size(); i++) {
                        if (!primaryKeyFields.contains(index.getFields().get(i))) {
                            matches = false;
                            break;
                        }
                    }

                    if (matches) {
                        index.setType(SchemaIndexType.PRIMARY);
                        primaryKeyFields.clear();
                    } else {
                        addIndex = false;
                    }
                } else {
                    addIndex = false;
                }
            }

            if (addIndex)
                table.getIndexes().add(index);
        }

        // Load the foreign keys.

        try (
            Statement stmt = getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("pragma foreign_key_list(" + escape(tableName) + ")")
        ) {
            DataSchemaForeignKey foreignKey = null;
            int nextSequence = 0;

            while (rs.next()) {
                // Check that the seq are sorted.
                int sequence = rs.getInt("seq");
                if (sequence != nextSequence)
                    throw new SchemaMigrateException("Invalid seq when reading foreign_key_list");
                nextSequence++;

                // Create the foreign key when we see a new sequence.

                if (sequence == 0) {
                    foreignKey = new DataSchemaForeignKey();
                    table.getForeignKeys().add(foreignKey);

                    // And initialize the new foreign key.

                    foreignKey.setName(String.format("FK_%s_%s", tableName, rs.getInt("id")));
                }

                if (sequence > 0)
                    throw new SchemaMigrateException("Composite foreign keys are not supported");

                foreignKey.setField(rs.getString("from"));
                foreignKey.setLinkTable(rs.getString("table"));
                foreignKey.setLinkField(rs.getString("to"));
            }
        } catch (SQLException e) {
            // foreign_key_list will throw "query does not return ResultSet" when it doesn't
            // have results.

            assert(e.getMessage().equals("query does not return ResultSet"));
        }

        return table;
    }

    private String escape(String tableName) {
        return "'" + tableName.replace("'", "''") + "'";
    }

    private SchemaDbType parseType(String typeName) {
        // Detecting the data types is done in two steps. First, the
        // ADO.Net mapping names as described in http://sqlite.phxsoftware.com/forums/t/31.aspx
        // are tried. If that fails, the SQLite's type rules as described
        // in http://www.sqlite.org/datatype3.html are tried. Note that we
        // use Decimal as the NUMERIC affinity because that is
        // the data type we're going to write. We use DECIMAL as numeric
        // because it follows the rules as described below.

        if (StringUtils.isEmpty(typeName))
            return SchemaDbType.BLOB;

        typeName = typeName.toUpperCase();

        switch (typeName)
        {
            case "BLOB":
            case "BINARY":
            case "VARBINARY":
            case "IMAGE":
            case "GENERAL":
            case "OLEOBJECT":
                return SchemaDbType.BLOB;

            case "BIT":
            case "YESNO":
            case "LOGICAL":
            case "BOOL":
                return SchemaDbType.BIT;

            case "TINYINT":
                return SchemaDbType.TINY_INT;

            case "TIME":
            case "DATE":
            case "TIMESTAMP":
            case "DATETIME":
            case "SMALLDATE":
            case "SMALLDATETIME":
                return SchemaDbType.DATE_TIME;

            case "NUMERIC":
            case "DECIMAL":
            case "MONEY":
            case "CURRENCY":
                return SchemaDbType.DECIMAL;

            case "DOUBLE":
            case "FLOAT":
                return SchemaDbType.DOUBLE;

            case "GUID":
            case "UNIQUEIDENTIFIER":
                return SchemaDbType.GUID;

            case "SMALLINT":
                return SchemaDbType.SMALL_INT;

            case "INT":
                return SchemaDbType.INT;

            case "COUNTER":
            case "AUTOINCREMENT":
            case "IDENTITY":
            case "LONG":
            case "INTEGER":
            case "BIGINT":
                return SchemaDbType.BIG_INT;

            case "REAL":
            case "VARCHAR":
            case "NVARCHAR":
            case "CHAR":
            case "NCHAR":
            case "TEXT":
            case "NTEXT":
            case "STRING":
            case "MEMO":
            case "NOTE":
            case "LONGTEXT":
            case "LONGCHAR":
            case "LONGVARCHAR":
                return SchemaDbType.TEXT;

            default:
                if (typeName.contains("INT"))
                    return SchemaDbType.INT;
                else if (typeName.contains("CHAR") || typeName.contains("CLOB") || typeName.contains("TEXT"))
                    return SchemaDbType.TEXT;
                else if (typeName.contains("BLOB"))
                    return SchemaDbType.BLOB;
                else if (typeName.contains("REAL") || typeName.contains("FLOA") || typeName.contains("DOUB"))
                    return SchemaDbType.DOUBLE;
                else
                    return SchemaDbType.DECIMAL;
        }
    }
}
