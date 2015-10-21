package nl.gmt.data.drivers;

import nl.gmt.data.DbConfiguration;
import nl.gmt.data.migrate.SchemaMigrateException;
import nl.gmt.data.migrate.SqlGenerator;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaRules;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.MySQL5Dialect;

public class MySqlDatabaseDriver extends GenericDatabaseDriver {
    @Override
    public String getConnectionType() {
        return "com.mysql.jdbc.Driver";
    }

    @Override
    public String getDialectType() {
        return MySQL5Dialect.class.getName();
    }

    @Override
    public boolean skipNullInUniqueIndex() {
        return true;
    }

    @Override
    public SqlGenerator createSqlGenerator(Schema schema) throws SchemaMigrateException {
        return new nl.gmt.data.migrate.mysql.SqlGenerator(schema);
    }

    @Override
    public SchemaRules createSchemaRules() throws SchemaMigrateException {
        return new nl.gmt.data.migrate.mysql.SchemaRules();
    }

    @Override
    public void createConfiguration(StandardServiceRegistryBuilder serviceRegistryBuilder, DbConfiguration configuration) {
        configureConnectionPooling(serviceRegistryBuilder, configuration, "SELECT 1;");
    }
}
