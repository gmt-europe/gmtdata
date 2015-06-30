package nl.gmt.data.migrate.postgres;

import nl.gmt.data.DataException;
import nl.gmt.data.migrate.*;
import nl.gmt.data.schema.*;
import org.apache.commons.lang.StringUtils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SqlGenerator extends GuidedSqlGenerator {
    private SchemaRules rules;
    private boolean closed;
    private Connection connection;

    public SqlGenerator(Schema schema) {
        super(schema);
    }

    @Override
    protected nl.gmt.data.schema.SchemaRules getRules() {
        return rules;
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
    public String getStatementSeparator() {
        return ";";
    }

    @Override
    protected String escapeComment(String comment) {
        return "-- " + comment;
    }

    @Override
    protected DataSchemaReader createDataSchemaReader() throws SchemaMigrateException {
        return new DataSchemaReader(connection);
    }

    @Override
    protected void writeUseStatement() throws SchemaMigrateException {
        // Does not apply to Postgres. The database is associated with the connection string.
        // However, we can misuse this call to emit loading the citext extension.

        // First see whether the citext data type is actually in use.

        if (isCiTextInUse()) {
            // Emit the create extension statement.
            addPrologStatement("CREATE EXTENSION IF NOT EXISTS citext");
        }
    }

    private boolean isCiTextInUse() {
        for (Map.Entry<String, SchemaClass> schemaClass : getSchema().getClasses().entrySet()) {
            if (classUsesCiText(schemaClass.getValue())) {
                return true;
            }
        }

        for (Map.Entry<String, SchemaMixin> mixin : getSchema().getMixins().entrySet()) {
            if (classUsesCiText(mixin.getValue())) {
                return true;
            }
        }

        return false;
    }

    private boolean classUsesCiText(SchemaClassBase schemaClass) {
        for (Map.Entry<String, SchemaProperty> property : schemaClass.getProperties().entrySet()) {
            if (property.getValue().getResolvedDataType().getDbType() == SchemaDbType.CASE_INSENSITIVE_TEXT) {
                return true;
            }
        }

        return false;
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

    private void writeAlterTable(DataSchemaTable table) {
        pushStatement("ALTER TABLE \"%s\"", table.getName());
    }

    @Override
    protected void writeDropForeignKey(DataSchemaTable table, String foreign) {
        writeAlterTable(table);

        addStatement("  DROP CONSTRAINT \"%s\"", foreign);
    }

    @Override
    protected void writeForeignKeyCreate(DataSchemaTable table, DataSchemaForeignKey foreign) {
        writeAlterTable(table);

        addStatement("  ADD FOREIGN KEY (\"%s\") REFERENCES \"%s\" (\"%s\")",
            foreign.getField(),
            foreign.getLinkTable(),
            foreign.getLinkField()
        );
    }

    @Override
    protected void writeTableCreate(DataSchemaTable table) throws SchemaMigrateException {
        pushStatement("CREATE TABLE \"%s\" (", table.getName());

        List<String> lines = new ArrayList<>();

        for (DataSchemaField field : table.getFields().values()) {
            lines.add(writeField(field));
        }

        for (DataSchemaIndex index : table.getIndexes()) {
            if (index.getType() == SchemaIndexType.PRIMARY) {
                lines.add(String.format("PRIMARY KEY (\"%s\")", StringUtils.join(index.getFields(), "\", \"")));
            }
        }

        for (int i = 0; i < lines.size(); i++) {
            StringBuilder sb = new StringBuilder();

            sb.append(String.format("  %s", lines.get(i)));

            if (i != lines.size() - 1) {
                sb.append(",");
            }

            pushStatement(sb.toString());
        }

        pushStatement(")");
        addStatement("WITH (OIDS = FALSE)");

        for (DataSchemaIndex index : table.getIndexes()) {
            if (index.getType() != SchemaIndexType.PRIMARY) {
                writeIndexCreate(table, index);
            }
        }
    }

    @Override
    protected void writeTableUpdate(DataSchemaTableDifference table) {
        throw new IllegalStateException("Postgres does not support extensions");
    }

    @Override
    protected void writeFieldDrop(DataSchemaTable table, String field) {
        writeAlterTable(table);

        addStatement("  DROP \"%s\"", field);
    }

    @Override
    protected void writeFieldUpdate(DataSchemaTable table, DataSchemaFieldDifference field) throws SchemaMigrateException {
        if (!field.getField().getName().equals(field.getOldField().getName())) {
            writeAlterTable(table);

            addStatement("  RENAME COLUMN \"%s\" TO \"%s\"", field.getOldField().getName(), field.getField().getName());
        }

        String type = getFieldType(field.getField());
        String oldType = getFieldType(field.getOldField());

        if (!type.equals(oldType)) {
            writeAlterTable(table);

            addStatement("  ALTER COLUMN \"%s\" TYPE %s", field.getField().getName(), type);
        }

        if (field.getField().isNullable() != field.getOldField().isNullable()) {
            writeAlterTable(table);

            if (field.getField().isNullable()) {
                addStatement("  ALTER COLUMN \"%s\" DROP NOT NULL", field.getField().getName());
            } else {
                addStatement("  ALTER COLUMN \"%s\" SET NOT NULL", field.getField().getName());
            }
        }
    }

    private String getFieldType(DataSchemaField field) throws SchemaMigrateException {
        StringBuilder sb = new StringBuilder();

        if (field.isAutoIncrement()) {
            sb.append(getAutoIncrementType(field.getType()));
        } else {
            sb.append(rules.getDbType(field.getType()));
        }

        sb.append(dataTypeLength(field));

        for (int i = 0; i < field.getArity(); i++) {
            sb.append("[]");
        }

        return sb.toString();
    }

    private String writeField(DataSchemaField field) throws SchemaMigrateException {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("\"%s\" %s", field.getName(), getFieldType(field)));

        if (!field.isNullable()) {
            sb.append(" NOT NULL");
        }

        return sb.toString();
    }

    private String getAutoIncrementType(SchemaDbType type) throws SchemaMigrateException {
        switch (type) {
            case SMALL_INT: return "SMALLSERIAL";
            case INT: return "SERIAL";
            default: throw new SchemaMigrateException(String.format("No auto increment type exists for type '%s'", type));
        }
    }

    private String dataTypeLength(DataSchemaField field) throws SchemaMigrateException {
        int length = field.getLength();

        int fixedLength = rules.getFixedDbTypeLength(field.getType());

        if (fixedLength != -1) {
            length = fixedLength;
        }

        if (length == -1) {
            if (dbTypeRequiresLength(field.getType())) {
                throw new SchemaMigrateException(String.format("Data type '%s' requires a length", field.getType()));
            } else {
                return "";
            }
        }

        if (!rules.dbTypeSupportsLength(field.getType())) {
            return "";
        }

        if (field.getPositions() == -1) {
            return String.format("(%d)", length);
        }

        return String.format("(%d,%d)", length, field.getPositions());
    }

    @Override
    protected void writeFieldCreate(DataSchemaTable table, DataSchemaField field) throws SchemaMigrateException {
        writeAlterTable(table);

        addStatement("  ADD COLUMN %s", writeField(field));
    }

    @Override
    protected void writeIndexDrop(DataSchemaTable table, String index) {
        addStatement("DROP INDEX \"%s\"", index);
    }

    @Override
    protected void writeIndexCreate(DataSchemaTable table, DataSchemaIndex index) throws SchemaMigrateException {
        assert index.getType() == SchemaIndexType.INDEX || index.getType() == SchemaIndexType.UNIQUE;

        addStatement(
            "CREATE %s \"%s\" ON \"%s\" USING %s (\"%s\")",
            index.getType() == SchemaIndexType.INDEX ? "INDEX" : "UNIQUE INDEX",
            index.getName(),
            table.getName(),
            rules.getIndexStrategy(index.getStrategy()),
            StringUtils.join(index.getFields(), "\", \"")
        );
    }

    @Override
    protected void writeTableDrop(String table) {
        addStatement("DROP TABLE \"%s\"", table);
        addNewline();
    }
}
