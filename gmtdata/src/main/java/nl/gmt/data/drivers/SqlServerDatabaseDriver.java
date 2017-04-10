package nl.gmt.data.drivers;

import nl.gmt.data.DbConfiguration;
import nl.gmt.data.migrate.SchemaMigrateException;
import nl.gmt.data.migrate.SqlGenerator;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaRules;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.SQLServer2012Dialect;

public class SqlServerDatabaseDriver extends GenericDatabaseDriver {
    @Override
    public String getConnectionType() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    @Override
    public String getDialectType() {
        return SQLServer2012Dialect.class.getName();
    }

    @Override
    public boolean skipNullInUniqueIndex() {
        return false;
    }

    @Override
    public SqlGenerator createSqlGenerator(Schema schema) throws SchemaMigrateException {
        return new nl.gmt.data.migrate.sqlserver.SqlGenerator(schema);
    }

    @Override
    public SchemaRules createSchemaRules() throws SchemaMigrateException {
        return new nl.gmt.data.migrate.sqlserver.SchemaRules();
    }

    @Override
    public void createConfiguration(StandardServiceRegistryBuilder serviceRegistryBuilder, DbConfiguration configuration) {
        configureConnectionPooling(serviceRegistryBuilder, configuration, "SELECT 1;");
    }
}
