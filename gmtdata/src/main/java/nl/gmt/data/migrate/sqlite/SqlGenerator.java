package nl.gmt.data.migrate.sqlite;

import nl.gmt.data.DataException;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaDbType;
import nl.gmt.data.schema.SchemaIndexType;
import nl.gmt.data.migrate.*;
import org.apache.commons.lang.StringUtils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SqlGenerator extends nl.gmt.data.migrate.SqlGenerator {
    private Connection connection;
    private boolean closed;
    private SchemaRules rules;

    public SqlGenerator(Schema schema) {
        super(schema);
    }

    @Override
    public String getStatementSeparator() {
        return ";";
    }

    @Override
    public void openConnection(DataSchemaExecutor executor) throws SchemaMigrateException {
        rules = (SchemaRules)executor.getRules();

        try {
            connection = executor.getConfiguration().getDatabaseDriver().createConnection(
                executor.getConfiguration().getConnectionString()
            );
        } catch (DataException e) {
            throw new SchemaMigrateException("Cannot open connection", e);
        }
    }

    @Override
    protected String escapeComment(String comment) {
        return "-- " + comment;
    }

    @Override
    protected DataSchemaReader createDataSchemaReader() {
        return new DataSchemaReader(connection);
    }

    @Override
    protected void writeUseStatement() {
        // SQLite does not support a current database.
    }

    @Override
    protected void applyChanges() throws SchemaMigrateException {
        // Foreign keys must be turned of when migrating the database. The
        // reason for this is that if we don't do this, renaming tables will
        // update the table name of the foreign key reference.

        addPrologStatement("PRAGMA foreign_keys = OFF");

        writeTableDeletes();

        writeTableCreates();

        writeTableUpdates();
    }

    private void writeTableDeletes() {
        boolean hadOne = false;

        for (String tableName : getDifference().getRemovedTables()) {
            if (!hadOne) {
                writeHeader("Removed tables");
                hadOne = true;
            }

            writeTableDrop(tableName);
        }
    }

    private void writeTableDrop(String tableName) {
        addStatement("DROP TABLE `%s`", tableName);
    }

    private void writeTableCreates() throws SchemaMigrateException {
        boolean hadOne = false;

        for (DataSchemaTable table : getDifference().getNewTables().values()) {
            if (!hadOne) {
                writeHeader("New tables");
                hadOne = true;
            }

            for (DataSchemaIndex index : table.getIndexes()) {
                index.createName(table, null);
            }

            writeTableCreate(table);
            addNewline();
        }
    }

    private void writeTableCreate(DataSchemaTable table) throws SchemaMigrateException {
        pushStatement("CREATE TABLE `%s` (", table.getName());

        List<String> lines = new ArrayList<>();

        String primaryKey = getPrimaryKeyField(table);

        for (DataSchemaField field : table.getFields().values()) {
            boolean isPrimary =
                primaryKey != null &&
                StringUtils.equalsIgnoreCase(field.getName(),  primaryKey);

            lines.add(writeField(table, field, isPrimary, getFieldForeignKey(table, field)));
        }

        for (int i = 0; i < lines.size(); i++) {
            StringBuilder sb = new StringBuilder();

            sb.append(String.format("  %s", lines.get(i)));

            if (i != lines.size() - 1)
            {
                sb.append(",");
            }

            pushStatement(sb.toString());
        }

        addStatement(")");

        for (DataSchemaIndex index : table.getIndexes()) {
            if (index.getType() != SchemaIndexType.PRIMARY)
                writeIndexCreate(table, index);
        }
    }

    private DataSchemaForeignKey getFieldForeignKey(DataSchemaTable table, DataSchemaField field) {
        for (DataSchemaForeignKey foreignKey : table.getForeignKeys()) {
            if (StringUtils.equalsIgnoreCase(field.getName(), foreignKey.getField()))
                return foreignKey;
        }

        return null;
    }

    private String writeField(DataSchemaTable table, DataSchemaField field, boolean isPrimary, DataSchemaForeignKey foreignKey) throws SchemaMigrateException {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("`%s` %s%s", field.getName(), rules.getDbType(field.getType()), dataTypeLength(field)));

        if (field.isAutoIncrement() && !isPrimary && SchemaRules.simplifyType(field.getType()) == SchemaDbType.INT)
            throw new SchemaMigrateException(String.format("SQLite requires auto increment field of table '%s' to be a primary key", table.getName()));

        if (isPrimary)
            sb.append(" PRIMARY KEY");

        if (!field.isNullable())
            sb.append(" NOT NULL");

        if (shouldCollateNocase(field.getType()))
            sb.append(" COLLATE NOCASE");

        if (foreignKey != null) {
            sb.append(String.format(
                " REFERENCES `%s` (`%s`)",
                foreignKey.getLinkTable(), foreignKey.getLinkField()
            ));
        }

        return sb.toString();
    }

    private boolean shouldCollateNocase(SchemaDbType type) throws SchemaMigrateException {
        // For compatibility, we set everything to not case sensitive.

        switch (SchemaRules.simplifyType(type)) {
            case TEXT:
            case GUID:
                return true;

            default:
                return false;
        }
    }

    private String dataTypeLength(DataSchemaField field) throws SchemaMigrateException {
        int length = field.getLength();

        int fixedLength = rules.getFixedDbTypeLength(field.getType());

        if (fixedLength != -1)
            length = fixedLength;

        if (length == -1) {
            if (dbTypeRequiresLength(field.getType()))
                throw new SchemaMigrateException(String.format("Data type '%s' requires a length", field.getType()));

            return "";
        }

        if (!rules.dbTypeSupportsLength(field.getType()))
            return "";

        if (field.getPositions() == -1)
            return String.format("(%d)", length);

        return String.format("(%d,%d)", length, field.getPositions());
    }

    private void writeIndexCreate(DataSchemaTable table, DataSchemaIndex index) throws SchemaMigrateException {
        StringBuilder sb = new StringBuilder();

        sb.append("CREATE ");

        switch (index.getType())
        {
            case INDEX:
                break;

            case UNIQUE:
                sb.append("UNIQUE ");
                break;

            default:
                throw new SchemaMigrateException(String.format("Unexpected index type '%s'", index.getType()));
        }

        sb.append("INDEX ");

        String indexName = index.getName();

        if (StringUtils.isEmpty(indexName)) {
            String indexNamePrefix = "IX_" + table.getName() + "_" + StringUtils.join(index.getFields(), "_");

            indexName = indexNamePrefix;
            int i = 1;

            while (indexNameInUse(table, indexName)) {
                indexName = indexNamePrefix + "_" + i++;
            }
        }

        sb.append("`");
        sb.append(indexName);
        sb.append("` ON `");
        sb.append(table.getName());
        sb.append("` (");

        for (int i = 0; i < index.getFields().size(); i++)
        {
            if (i > 0)
                sb.append(", ");

            sb.append("`");
            sb.append(index.getFields().get(i));
            sb.append("`");

            if (shouldCollateNocase(table.getFields().get(index.getFields().get(i)).getType()))
                sb.append(" COLLATE NOCASE");
        }

        sb.append(")");

        addStatement(sb.toString());
    }

    private boolean indexNameInUse(DataSchemaTable table, String indexName) {
        for (DataSchemaIndex index : table.getIndexes()) {
            if (StringUtils.equalsIgnoreCase(indexName, index.getName()))
                return true;
        }

        for (DataSchemaTable currentTable : getCurrentSchema().getTables().values()) {
            for (DataSchemaIndex index : currentTable.getIndexes()) {
                if (StringUtils.equalsIgnoreCase(indexName, index.getName()))
                    return true;
            }
        }

        return false;
    }

    private String getPrimaryKeyField(DataSchemaTable table) throws SchemaMigrateException {
        DataSchemaIndex primaryKey = null;

        for (DataSchemaIndex index : table.getIndexes()) {
            if (index.getType() == SchemaIndexType.PRIMARY) {
                primaryKey = index;
                break;
            }
        }

        if (primaryKey == null)
            return null;

        if (primaryKey.getFields().size() != 1)
            throw new SchemaMigrateException(String.format("SQLite does not support primary keys on multiple fields for table '%s'", table.getName()));

        return primaryKey.getFields().get(0);
    }

    private void writeTableUpdates() throws SchemaMigrateException {
        boolean hadOne = false;

        for (DataSchemaTableDifference table : getDifference().getChangedTables().values()) {
            if (!hadOne) {
                writeHeader("Update existing tables");
                hadOne = true;
            }

            if (requiresRecreate(table))
                recreateTable(table);
            else
                applyTableChanges(table);
        }
    }

    private boolean requiresRecreate(DataSchemaTableDifference table) {
        return
            table.getChangedFields().size() > 0 ||
            table.getNewForeignKeys().size() > 0 ||
            table.getRemovedFields().size() > 0 ||
            table.getRemovedForeignKeys().size() > 0;
    }

    private void recreateTable(DataSchemaTableDifference table) throws SchemaMigrateException {
        String tempTableName = table.getSchema().getName() + "_" + UUID.randomUUID().toString().replace("-", "");

        addStatement("ALTER TABLE `%s` RENAME TO `%s`", table.getSchema().getName(), tempTableName);

        writeTableCreate(table.getSchema());

        // Transport all records from the old to the new table copying
        // fields that appear in both the old and new schema.

        List<String> fieldList = new ArrayList<>();

        for (String fieldName : table.getSchema().getFields().keySet()) {
            if (table.getOldSchema().getFields().containsKey(fieldName))
                fieldList.add(String.format("`%s`", fieldName));
        }

        String fields = StringUtils.join(fieldList, ", ");

        pushStatement("INSERT INTO `%s` (%s)", table.getSchema().getName(), fields);
        pushStatement("  SELECT %s", fields);
        addStatement("  FROM `%s`", tempTableName);

        writeTableDrop(tempTableName);
    }

    private void applyTableChanges(DataSchemaTableDifference table) throws SchemaMigrateException {
        // RequiresRecreate verified that we only have to worry about
        // the items that are processed here. If there are more changes,
        // we recreate the table anyway.

        for (String index : table.getRemovedIndexes()) {
            writeIndexDrop(index);
        }

        for (DataSchemaField field : table.getNewFields().values()) {
            writeFieldCreate(table.getSchema(), field);
        }

        for (DataSchemaIndex index : table.getNewIndexes()) {
            writeIndexCreate(table.getSchema(), index);
        }
    }

    private void writeIndexDrop(String index) {
        addStatement("DROP INDEX `%s`", index);
    }

    private void writeFieldCreate(DataSchemaTable table, DataSchemaField field) throws SchemaMigrateException {
        writeAlterTable(table);

        String primaryKey = getPrimaryKeyField(table);

        boolean isPrimary =
            primaryKey != null &&
            StringUtils.equalsIgnoreCase(field.getName(), primaryKey);

        String text = writeField(table, field, isPrimary, getFieldForeignKey(table, field));

        if (!field.isNullable())
        {
            String defaultValue;

            switch (SchemaRules.simplifyType(field.getType()))
            {
                case INT:
                case SMALL_INT:
                case TINY_INT:
                case DOUBLE:
                case DECIMAL:
                    defaultValue = "0";
                    break;

                default:
                    defaultValue = "''";
                    break;
            }

            text += " DEFAULT " + defaultValue;
        }

        addStatement("  ADD %s", text);
    }

    private void writeAlterTable(DataSchemaTable table) {
        pushStatement("ALTER TABLE `%s`", table.getName());
    }

    @Override
    public void close() throws Exception {
        if (!closed) {
            if (connection != null) {
                connection.close();
                connection = null;
            }

            closed = true;
        }
    }
}
