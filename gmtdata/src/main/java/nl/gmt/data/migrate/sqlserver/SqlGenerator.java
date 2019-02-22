package nl.gmt.data.migrate.sqlserver;

import nl.gmt.data.DataException;
import nl.gmt.data.migrate.*;
import nl.gmt.data.migrate.DataSchemaReader;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaIndexType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SqlGenerator extends GuidedSqlGenerator {
    private nl.gmt.data.migrate.sqlserver.SchemaRules rules;
    private boolean closed;
    private Connection connection;

    public SqlGenerator(Schema schema) {
        super(schema);
    }

    @Override
    public void openConnection(DataSchemaExecutor executor) throws SchemaMigrateException {
        rules = (nl.gmt.data.migrate.sqlserver.SchemaRules)executor.getRules();

        try {
            connection = executor.getConfiguration().getDatabaseDriver().createConnection(
                executor.getConfiguration().getConnectionString()
            );
        } catch (DataException e) {
            throw new SchemaMigrateException("Cannot open connection", e);
        }
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

    @Override
    public String getStatementSeparator() {
        return "\nGO";
    }

    @Override
    protected String escapeComment(String comment) {
        return "/* " + comment + " */";
    }

    @Override
    protected nl.gmt.data.schema.SchemaRules getRules() {
        return rules;
    }

    @Override
    protected void writeUseStatement() throws SchemaMigrateException {
        String database = null;

        try (
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT db_name()")
        ) {
            while (rs.next()) {
                database = rs.getString(1);
            }
        } catch (SQLException e) {
            throw new SchemaMigrateException("Cannot get database name", e);
        }

        addStatement(SqlStatementType.USE_STATEMENT, "USE [%s]", database);
    }

    @Override
    protected DataSchemaReader createDataSchemaReader() throws SchemaMigrateException {
        return new nl.gmt.data.migrate.sqlserver.DataSchemaReader(connection);
    }

    @Override
    protected void applyChanges() throws SchemaMigrateException {
        writeSupportTable();

        super.applyChanges();
    }

    private void writeSupportTable() throws SchemaMigrateException {
        try (InputStream is = getClass().getResourceAsStream("GmtDataSupport.sql")) {
            String sql = IOUtils.toString(is, StandardCharsets.UTF_8);
            addPrologStatement(sql);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void writeDropForeignKey(DataSchemaTable table, String foreign) {
        writeAlterTable(table);
        addStatement("  DROP CONSTRAINT [%s]", foreign);
    }

    @Override
    protected void writeIndexCreate(DataSchemaTable table, DataSchemaIndex index) throws SchemaMigrateException {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE");
        switch (index.getType()) {
            case INDEX:
                break;
            case UNIQUE:
                sb.append(" UNIQUE");
                break;
            default:
                throw new IllegalStateException();
        }

        if (index.getStrategy() != null) {
            sb.append(' ').append(index.getStrategy());
        }
        sb.append(" INDEX [").append(index.getName()).append("]");
        sb
            .append(" ON [dbo].[").append(table.getName()).append("] (")
            .append(getFieldList(index.getFields())).append(")");
        if (index.getIncludeFields().size() > 0) {
            sb
                .append(" INCLUDE (")
                .append(getFieldList(index.getIncludeFields())).append(")");
        }
        if (index.getFilter() != null) {
            sb.append(" WHERE ").append(index.getFilter());
        }
        sb
            .append(" ON [PRIMARY]");
        addStatement(sb.toString());
        if (index.getFilter() != null) {
            addStatement(
                "INSERT INTO [dbo].[gmtdataschema] ([key], [value]) " +
                    "VALUES (%s, %s)",
                SqlUtil.escape(table.getName() + "::" + index.getName() + "::filter"),
                SqlUtil.escape(index.getFilter())
            );
        }
    }

    @Override
    protected void writeIndexDrop(DataSchemaTable table, String index) {
        addStatement("DELETE FROM [dbo].[gmtdataschema] WHERE [key] LIKE %s", SqlUtil.escape(table.getName() + "::" + index + "::%"));
        addStatement("DROP INDEX [dbo].[%s].[%s]", table.getName(), index);
    }

    @Override
    protected void writeFieldCreate(DataSchemaTable table, DataSchemaField field) throws SchemaMigrateException {
        writeAlterTable(table);
        addStatement("  ADD %s", writeField(field));
    }

    @Override
    protected void writeFieldUpdate(DataSchemaTable table, DataSchemaFieldDifference field) throws SchemaMigrateException {
        List<DataSchemaIndex> recreateIndexes = new ArrayList<>();
        DataSchemaTable currentTable = getCurrentSchema().getTables().get(table.getName());
        DataSchemaTableDifference tableDifference = getDifference().getChangedTables().get(table.getName());

        for (DataSchemaIndex index : currentTable.getIndexes()) {
            boolean found =
                hasField(field, index.getFields()) ||
                hasField(field, index.getIncludeFields());

            if (found && tableDifference != null) {
                for (String removedIndex : tableDifference.getRemovedIndexes()) {
                    if (StringUtils.equalsIgnoreCase(removedIndex, index.getName())) {
                        found = false;
                    }
                }
            }
            if (found) {
                recreateIndexes.add(index);
                writeIndexDrop(table, index.getName());
            }
        }
        if (!field.getField().getName().equals(field.getOldField().getName())) {
            writeAlterTable(table);
            addStatement("  RENAME COLUMN [%s] TO [%s]", field.getOldField().getName(), field.getField().getName());
        }
        writeAlterTable(table);
        addStatement("  ALTER COLUMN %s", writeField(field.getField()));
        for (DataSchemaIndex index : recreateIndexes) {
            writeIndexCreate(table, index);
        }
    }

    private boolean hasField(DataSchemaFieldDifference field, List<String> fields) {
        for (String indexField : fields) {
            if (StringUtils.equalsIgnoreCase(indexField, field.getOldField().getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void writeFieldDrop(DataSchemaTable table, String field) {
        writeAlterTable(table);
        addStatement("  DROP COLUMN [%s]", field);
    }

    @Override
    protected void writeForeignKeyCreate(DataSchemaTable table, DataSchemaForeignKey foreign) {
        writeAlterTable(table);
        addStatement(
            "  ADD CONSTRAINT [%s] FOREIGN KEY (%s) REFERENCES [dbo].[%s] (%s)",
            foreign.getName(),
            foreign.getField(),
            foreign.getLinkTable(),
            foreign.getLinkField()
        );
    }

    private String getFieldList(List<String> fields) {
        StringBuilder sb = new StringBuilder();
        for (String field : fields) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append('[').append(field).append(']');
        }
        return sb.toString();
    }

    @Override
    protected void writeTableCreate(DataSchemaTable table) throws SchemaMigrateException {
        pushStatement("CREATE TABLE [dbo].[%s] (", table.getName());
        List<String> lines = new ArrayList<>();
        for (DataSchemaField field : table.getFields().values()) {
            lines.add(writeField(field));
        }
        for (DataSchemaIndex index : table.getIndexes()) {
            if (index.getType() == SchemaIndexType.PRIMARY) {
                lines.add(writeIndex(index));
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("  ").append(lines.get(i));
            if (i != lines.size() - 1) {
                sb.append(",");
            }
            pushStatement(sb.toString());
        }
        addStatement(")");
        for (DataSchemaIndex index : table.getIndexes()) {
            if (index.getType() != SchemaIndexType.PRIMARY) {
                writeIndexCreate(table, index);
            }
        }
    }

    @Override
    protected void writeTableUpdate(DataSchemaTableDifference table) {
        // Unused.
    }

    @Override
    protected void writeTableDrop(String table) {
        addStatement("DELETE FROM [dbo].[gmtdataschema] WHERE [key] LIKE %s", SqlUtil.escape(table + "::%"));
        addStatement("DROP TABLE [dbo].[%s]", table);
        addNewline();
    }

    private void writeAlterTable(DataSchemaTable table) {
        pushStatement("ALTER TABLE [dbo].[%s]", table.getName());
    }

    private String writeField(DataSchemaField field) throws SchemaMigrateException {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(field.getName()).append("] ").append(getDataType(field));
        if (field.isAutoIncrement()) {
            sb.append(" IDENTITY(1, 1)");
        }
        if (!field.isNullable()) {
            sb.append(" NOT NULL");
        }
        return sb.toString();
    }

    private String getDataType(DataSchemaField field) throws SchemaMigrateException {
        String postfix = null;

        if (field.getLength() == -1) {
            switch (field.getType()) {
                case STRING:
                case BINARY:
                    postfix = "(MAX)";
                    break;
            }
        }

        if (postfix == null && rules.dbTypeSupportsLength(field.getType())) {
            if (field.getPositions() == -1) {
                postfix = String.format("(%s)", field.getLength());
            } else {
                postfix = String.format("(%d,%d)", field.getLength(), field.getPositions());
            }
        }

        String type = rules.getDbType(field.getType());
        if (postfix != null) {
            type += postfix;
        }

        return type;
    }

    private String writeIndex(DataSchemaIndex index) throws SchemaMigrateException {
        assert index.getType() == SchemaIndexType.PRIMARY;

        return String.format("PRIMARY KEY %s (%s)", rules.getIndexStrategy(index), getFieldList(index.getFields()));
    }
}
