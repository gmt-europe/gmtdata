package nl.gmt.data.drivers;

import nl.gmt.data.migrate.SchemaMigrateException;
import nl.gmt.data.migrate.SqlGenerator;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaRules;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.SQLServerDialect;

public class SqlServerDatabaseDriver extends GenericDatabaseDriver {
    @Override
    public String getConnectionType() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    @Override
    public String getDialectType() {
        return SQLServerDialect.class.getName();
    }

    @Override
    public SqlGenerator createSqlGenerator(Schema schema) throws SchemaMigrateException {
        throw new SchemaMigrateException("Migration is not supported for SQL Server");
    }

    @Override
    public SchemaRules createSchemaRules() throws SchemaMigrateException {
        return new nl.gmt.data.migrate.sqlserver.SchemaRules();
    }

    @Override
    public void createConfiguration(Configuration configuration) {
        // configureConnectionPooling(configuration, "SELECT 1;");
    }
}
