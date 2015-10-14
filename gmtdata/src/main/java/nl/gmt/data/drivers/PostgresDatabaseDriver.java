package nl.gmt.data.drivers;

import nl.gmt.data.DbConfiguration;
import nl.gmt.data.migrate.SchemaMigrateException;
import nl.gmt.data.migrate.SqlGenerator;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaRules;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.PostgreSQL9Dialect;

public class PostgresDatabaseDriver extends GenericDatabaseDriver {
    @Override
    public String getConnectionType() {
        return "org.postgresql.Driver";
    }

    @Override
    public String getDialectType() {
        return PostgreSQL9Dialect.class.getName();
    }

    @Override
    public SqlGenerator createSqlGenerator(Schema schema) throws SchemaMigrateException {
        return new nl.gmt.data.migrate.postgres.SqlGenerator(schema);
    }

    @Override
    public SchemaRules createSchemaRules() throws SchemaMigrateException {
        return new nl.gmt.data.migrate.postgres.SchemaRules();
    }

    @Override
    public void createConfiguration(Configuration cfg, DbConfiguration configuration) {
        configureConnectionPooling(cfg, configuration, "SELECT 1;");
    }
}
