package nl.gmt.data.drivers;

import nl.gmt.data.migrate.SchemaMigrateException;
import nl.gmt.data.migrate.SqlGenerator;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaRules;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.ProgressDialect;

public class ProgressDatabaseDriver extends GenericDatabaseDriver {
    @Override
    public String getConnectionType() {
        return "com.ddtek.jdbc.openedge.OpenEdgeDriver";
    }

    @Override
    public String getDialectType() {
        return ProgressDialect.class.getName();
    }

    @Override
    public SqlGenerator createSqlGenerator(Schema schema) throws SchemaMigrateException {
        throw new SchemaMigrateException("Migration is not supported for Progress");
    }

    @Override
    public SchemaRules createSchemaRules() throws SchemaMigrateException {
        return new nl.gmt.data.migrate.progress.SchemaRules();
    }

    @Override
    public void createConfiguration(Configuration configuration) {
        configureConnectionPooling(configuration, "SELECT 1;");
    }
}
