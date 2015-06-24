package nl.gmt.data.migrate;

import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaDbType;
import nl.gmt.data.schema.SchemaException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class SqlGenerator implements AutoCloseable {
    private List<SqlStatement> statements;
    private List<String> prologStatements;
    private final StringBuilder statementCache = new StringBuilder();
    private final Schema schema;
    private DataSchemaDifference different;
    private DataSchema currentSchema;
    private DataSchema newSchema;

    protected SqlGenerator(Schema schema) {
        this.schema = schema;
    }

    public Schema getSchema() {
        return schema;
    }

    public DataSchemaDifference getDifference() {
        return different;
    }

    public DataSchema getCurrentSchema() {
        return currentSchema;
    }

    public DataSchema getNewSchema() {
        return newSchema;
    }

    public abstract String getStatementSeparator();

    public abstract void openConnection(DataSchemaExecutor executor) throws SchemaMigrateException;

    public void execute(DataSchemaExecutor executor) throws SchemaMigrateException {
        statements = new ArrayList<>();
        prologStatements = new ArrayList<>();

        try {
            openConnection(executor);
        } catch (Throwable e) {
            throw new SchemaMigrateException("Cannot open connection", e);
        }

        writeOutput(executor);

        executor.getCallback().serializeSql(statements);
    }

    private void addComment(String comment, Object... args) {
        if (args != null && args.length > 0)
            comment = String.format(comment, args);

        statements.add(new SqlStatement(SqlStatementType.COMMENT, escapeComment(comment) + "\n"));
    }

    protected abstract String escapeComment(String comment);

    protected void addPrologStatement(String statement, Object... args) throws SchemaMigrateException {
        for (SqlStatement item : statements) {
            if (item.getType() == SqlStatementType.STATEMENT)
                throw new SchemaMigrateException("Prolog must proceed statements");
        }

        if (args != null && args.length > 0)
            statement = String.format(statement, args);

        prologStatements.add(statement);
    }

    protected void addStatement(String statement, Object... args) {
        addStatement(SqlStatementType.STATEMENT, statement, args);
    }

    protected void addStatement(SqlStatementType type, String statement, Object... args) {
        if (args != null && args.length > 0)
            statement = String.format(statement, args);

        if (type == SqlStatementType.STATEMENT && prologStatements.size() > 0) {
            for (String prologStatement : prologStatements) {
                statements.add(new SqlStatement(SqlStatementType.STATEMENT, prologStatement));
            }

            prologStatements.clear();
        }

        statementCache.append(statement);

        statements.add(new SqlStatement(type, statementCache.toString()));
        statements.add(new SqlStatement(SqlStatementType.SEPARATOR, getStatementSeparator() + "\n"));

        statementCache.setLength(0);
    }

    protected void pushStatement(String statement, Object... args) {
        if (args != null && args.length > 0)
            statement = String.format(statement, args);

        statementCache.append(statement);
        statementCache.append("\n");
    }

    protected void addNewline() {
        statements.add(new SqlStatement(SqlStatementType.COMMENT, "\n"));
    }

    private void writeOutput(DataSchemaExecutor executor) throws SchemaMigrateException {
        addComment("Automatically generated migration script");
        addComment("Generated at %s", new Date().toString());
        addNewline();

        writeUseStatement();

        addNewline();

        currentSchema = readDataSchema();
        newSchema = DataSchema.fromSchema(schema, executor.getRules());

        different = new DataSchemaDifference(currentSchema, newSchema, executor);

        applyChanges();
    }

    protected abstract void applyChanges() throws SchemaMigrateException;

    private DataSchema readDataSchema() throws SchemaMigrateException {
        return DataSchema.fromReader(createDataSchemaReader());
    }

    protected abstract DataSchemaReader createDataSchemaReader() throws SchemaMigrateException;

    protected abstract void writeUseStatement() throws SchemaMigrateException;

    protected void writeHeader(String header) {
        addComment(header);
        addNewline();
    }

    public static boolean dbTypeRequiresLength(SchemaDbType dbType) throws SchemaMigrateException {
        switch (dbType) {
            case STRING:
            case FIXED_STRING:
            case FIXED_BINARY:
            case BINARY:
                return true;

            default:
                return false;
        }
    }

    public static SchemaDbType getDbType(String dataType) throws SchemaException {
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
            case "varchar": return SchemaDbType.STRING;
            case "text": return SchemaDbType.TEXT;
            case "blob": return SchemaDbType.BLOB;
            case "datetime": return SchemaDbType.DATE_TIME;
            case "char": return SchemaDbType.FIXED_STRING;
            case "binary": return SchemaDbType.FIXED_BINARY;
            case "varbinary": return SchemaDbType.BINARY;
            case "tinyblob": return SchemaDbType.TINY_BLOB;
            case "tinytext": return SchemaDbType.TINY_TEXT;
            case "mediumblob": return SchemaDbType.MEDIUM_BLOB;
            case "mediumtext": return SchemaDbType.MEDIUM_TEXT;
            case "longblob": return SchemaDbType.LONG_BLOB;
            case "longtext": return SchemaDbType.LONG_TEXT;
            case "guid": return SchemaDbType.GUID;

            default:
                throw new SchemaException(String.format("Unexpected data type '%s'", dataType));
        }
    }
}
