package nl.gmt.data.test;

import nl.gmt.ReflectionUtils;
import nl.gmt.data.*;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.test.types.EntitySchema;

public class TestConnection extends DbConnection<EntitySchema> {
    public TestConnection(String connectionString, DbType type) throws DataException {
        super(
            connectionString,
            type,
            "Database.schema",
            new RepositoryService(ReflectionUtils.getUrlFromClass(TestConnection.class))
        );
    }

    @Override
    protected EntitySchema createEntitySchema(Schema schema) throws DataException {
        return new EntitySchema(schema);
    }
}
