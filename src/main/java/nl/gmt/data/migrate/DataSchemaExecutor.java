package nl.gmt.data.migrate;

import nl.gmt.data.schema.*;

public class DataSchemaExecutor {
    private final DataSchemaExecutorConfiguration configuration;
    private final SchemaCallback callback;
    private SchemaRules rules;

    public DataSchemaExecutor(DataSchemaExecutorConfiguration configuration, SchemaCallback callback) {
        this.configuration = configuration;
        this.callback = callback;
    }

    public DataSchemaExecutorConfiguration getConfiguration() {
        return configuration;
    }

    public SchemaCallback getCallback() {
        return callback;
    }

    public SchemaRules getRules() {
        return rules;
    }

    public void execute() throws SchemaMigrateException {
        SchemaParserExecutor parserExecutor = new SchemaParserExecutor(callback);

        try {
            execute(parserExecutor.parse(configuration.getSchemaFileName(), getRules()));
        } catch (SchemaException e) {
            throw new SchemaMigrateException("Cannot execute migration", e);
        }
    }

    public DatabaseSchema execute(Schema schema) throws SchemaMigrateException {
        rules = configuration.getDatabaseDriver().createSchemaRules();

        return new DatabaseSchema(schema, this);
    }
}
