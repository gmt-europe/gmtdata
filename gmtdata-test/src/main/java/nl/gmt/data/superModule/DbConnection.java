package nl.gmt.data.superModule;

import nl.gmt.data.DataException;
import nl.gmt.data.DbConfiguration;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.superModule.types.EntitySchema;

public class DbConnection extends nl.gmt.data.DbConnection<EntitySchema> {
    public DbConnection(DbConfiguration configuration) throws DataException {
        super(
            configuration,
            "Database.schema",
            null
        );
    }

    @Override
    protected EntitySchema createEntitySchema(Schema schema) throws DataException {
        return new EntitySchema(schema);
    }
}
