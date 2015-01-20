package nl.gmt.data.test;

import nl.gmt.data.support.ReflectionUtils;
import nl.gmt.data.DataException;
import nl.gmt.data.DbConfiguration;
import nl.gmt.data.RepositoryService;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.test.types.EntitySchema;
import nl.gmt.data.DbConnection;

public class TestConnection extends DbConnection<EntitySchema> {
    public TestConnection(DbConfiguration configuration) throws DataException {
        super(
            configuration,
            "Database.schema",
            new RepositoryService(ReflectionUtils.getUrlFromClass(TestConnection.class))
        );
    }

    @Override
    protected EntitySchema createEntitySchema(Schema schema) throws DataException {
        return new EntitySchema(schema);
    }
}
