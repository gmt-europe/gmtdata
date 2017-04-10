package nl.gmt.data.postgres2sqlserver;

import nl.gmt.data.migrate.SchemaMigrateException;
import nl.gmt.data.schema.SchemaDbType;
import nl.gmt.data.schema.SchemaRules;

class GenericSchemaRules extends SchemaRules {
    public static final GenericSchemaRules INSTANCE = new GenericSchemaRules();

    private GenericSchemaRules() {
    }

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
