package nl.gmt.data.migrate;

import nl.gmt.data.drivers.DatabaseDriver;
import nl.gmt.data.schema.Schema;

public class DatabaseSchema {
    private final DataSchema currentSchema;
    private final DataSchema newSchema;

    public DatabaseSchema(Schema schema, DataSchemaExecutor executor) throws SchemaMigrateException {
        try (SqlGenerator generator = executor.getConfiguration().getDatabaseDriver().createSqlGenerator(schema)) {
            generator.execute(executor);

            currentSchema = generator.getCurrentSchema();
            newSchema = generator.getNewSchema();
        } catch (Throwable e) {
            throw new SchemaMigrateException("Cannot create database schema", e);
        }
    }

    public DataSchema getCurrentSchema() {
        return currentSchema;
    }

    public DataSchema getNewSchema() {
        return newSchema;
    }
}
