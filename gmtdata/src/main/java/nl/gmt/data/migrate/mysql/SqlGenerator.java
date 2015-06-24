package nl.gmt.data.migrate.mysql;

import nl.gmt.data.DataException;
import nl.gmt.data.migrate.*;
import nl.gmt.data.schema.Schema;
import org.apache.commons.lang.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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
        String database = null;

        try (
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT DATABASE()")
        ) {
            while (rs.next()) {
                database = rs.getString(1);
            }
        } catch (SQLException e) {
            throw new SchemaMigrateException("Cannot get database name", e);
        }

        addStatement(SqlStatementType.USE_STATEMENT, "USE `%s`", database);
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
        pushStatement("ALTER TABLE `%s`", table.getName());
    }

    @Override
    protected void writeDropForeignKey(DataSchemaTable table, String foreign) {
        writeAlterTable(table);

        addStatement("  DROP FOREIGN KEY `%s`", foreign);
    }

    @Override
    protected void writeForeignKeyCreate(DataSchemaTable table, DataSchemaForeignKey foreign) {
        writeAlterTable(table);

        addStatement("  ADD CONSTRAINT FOREIGN KEY (`%s`) REFERENCES `%s` (`%s`)",
            foreign.getField(),
            foreign.getLinkTable(),
            foreign.getLinkField()
        );
    }

    @Override
    protected void writeTableCreate(DataSchemaTable table) throws SchemaMigrateException {
        pushStatement("CREATE TABLE `%s` (", table.getName());

        List<String> lines = new ArrayList<>();

        for (DataSchemaField field : table.getFields().values()) {
            lines.add(writeField(field, null));
        }

        for (DataSchemaIndex index : table.getIndexes()) {
            lines.add(writeIndex(index));
        }

        for (int i = 0; i < lines.size(); i++) {
            StringBuilder sb = new StringBuilder();

            sb.append(String.format("  %s", lines.get(i)));

            if (i != lines.size() - 1) {
                sb.append(",");
            }

            pushStatement(sb.toString());
        }

        addStatement(
            ") ENGINE=%s DEFAULT CHARSET=%s COLLATE=%s",
            SchemaRules.getEngine(table),
            SchemaRules.getDefaultCharset(table),
            SchemaRules.getDefaultCollation(table)
        );
    }

    private String writeIndex(DataSchemaIndex index) throws SchemaMigrateException {
        StringBuilder sb = new StringBuilder();

        switch (index.getType()) {
            case PRIMARY:
                sb.append(String.format("PRIMARY KEY (`%s`)", StringUtils.join(index.getFields(), "`, `")));
                break;

            case INDEX:
                sb.append(String.format("KEY (`%s`)", StringUtils.join(index.getFields(), "`, `")));
                break;

            case UNIQUE:
                sb.append(String.format("UNIQUE (`%s`)", StringUtils.join(index.getFields(), "`, `")));
                break;

            default:
                throw new SchemaMigrateException("Unknown index type");
        }

        return sb.toString();
    }

    @Override
    protected void writeTableUpdate(DataSchemaTableDifference table) {
        writeAlterTable(table.getSchema());

        List<String> parts = new ArrayList<>();

        if (!StringUtils.equals(SchemaRules.getDefaultCharset(table.getOldSchema()), SchemaRules.getDefaultCharset(table.getSchema()))) {
            parts.add(String.format("DEFAULT CHARSET %s", SchemaRules.getDefaultCharset(table.getSchema())));
        }
        if (!StringUtils.equals(SchemaRules.getDefaultCollation(table.getOldSchema()), SchemaRules.getDefaultCollation(table.getSchema()))) {
            parts.add(String.format("COLLATE %s", SchemaRules.getDefaultCollation(table.getSchema())));
        }
        if (!StringUtils.equals(SchemaRules.getEngine(table.getOldSchema()), SchemaRules.getEngine(table.getSchema()))) {
            parts.add(String.format("ENGINE %s", SchemaRules.getEngine(table.getSchema())));
        }

        addStatement("  %s", StringUtils.join(parts, " "));
    }

    @Override
    protected void writeFieldDrop(DataSchemaTable table, String field) {
        writeAlterTable(table);

        addStatement("  DROP `%s`", field);
    }

    @Override
    protected void writeFieldUpdate(DataSchemaTable table, DataSchemaFieldDifference field) throws SchemaMigrateException {
        writeAlterTable(table);

        addStatement("  CHANGE `%s` %s", field.getField().getName(),
            writeField(field.getField(), field.getOldField())
        );
    }

    private String writeField(DataSchemaField field, DataSchemaField oldField) throws SchemaMigrateException {
        if (field.getArity() > 0) {
            throw new SchemaMigrateException("MySQL does not support array types");
        }

        StringBuilder sb = new StringBuilder();

        sb.append(String.format("`%s` %s%s", field.getName(), rules.getDbType(field.getType()), dataTypeLength(field)));

        if (rules.dbTypeSupportsSign(field.getType()) && field.isUnsigned()) {
            sb.append(" UNSIGNED");
        }

        if (field.isAutoIncrement()) {
            sb.append(" AUTO_INCREMENT");
        }

        if (SchemaRules.dbTypeSupportsCharset(field.getType())) {
            if (oldField != null && !StringUtils.equals(SchemaRules.getCharset(oldField), SchemaRules.getCharset(field))) {
                sb.append(String.format(" CHARACTER SET %s", SchemaRules.getCharset(field)));
            }

            if (oldField != null && !StringUtils.equals(SchemaRules.getCollation(oldField), SchemaRules.getCollation(field))) {
                sb.append(String.format(" COLLATE %s", SchemaRules.getCollation(field)));
            }
        }

        if (!field.isNullable()) {
            sb.append(" NOT NULL");
        }

        return sb.toString();
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

        addStatement("  ADD %s", writeField(field, null));
    }

    @Override
    protected void writeIndexDrop(DataSchemaTable table, String index) {
        writeAlterTable(table);

        addStatement("  DROP INDEX `%s`", index);
    }

    @Override
    protected void writeIndexCreate(DataSchemaTable table, DataSchemaIndex index) throws SchemaMigrateException {
        writeAlterTable(table);

        String indexType;

        switch (index.getType()) {
            case INDEX:
                indexType = "INDEX";
                break;

            case UNIQUE:
                indexType = "UNIQUE";
                break;

            default:
                throw new SchemaMigrateException("Unexpected index type");
        }

        addStatement("  ADD %s (`%s`)", indexType, StringUtils.join(index.getFields(), "`, `"));
    }

    @Override
    protected void writeTableDrop(String table) {
        addStatement("DROP TABLE `%s`", table);
        addNewline();
    }
}
