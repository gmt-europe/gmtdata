package nl.gmt.data.migrate.sqlserver;

import nl.gmt.data.migrate.SchemaMigrateException;
import nl.gmt.data.schema.SchemaDbType;

/**
 * This is an empty placeholder for when migration support is implemented.
 */
public class SchemaRules extends nl.gmt.data.schema.SchemaRules {
    @Override
    public String getDbType(SchemaDbType dbType) throws SchemaMigrateException {
        throw new IllegalStateException("Not supported");
    }

    @Override
    public boolean dbTypeSupportsSign(SchemaDbType dbType) {
        return false;
    }

    @Override
    public boolean dbTypeSupportsLength(SchemaDbType dbType) {
        return false;
    }
}
