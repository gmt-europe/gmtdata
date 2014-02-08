package nl.gmt.data.drivers;

import nl.gmt.data.migrate.SchemaMigrateException;
import nl.gmt.data.migrate.SqlGenerator;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaRules;

public class SQLiteDatabaseDriver extends GenericDatabaseDriver {
    @Override
    public String getConnectionType() {
        return "org.sqlite.JDBC";
    }

    @Override
    public SqlGenerator createSqlGenerator(Schema schema) throws SchemaMigrateException {
        return new nl.gmt.data.migrate.sqlite.SqlGenerator(schema);
    }

    @Override
    public SchemaRules createSchemaRules() throws SchemaMigrateException {
        return new nl.gmt.data.migrate.sqlite.SchemaRules();
    }
}
