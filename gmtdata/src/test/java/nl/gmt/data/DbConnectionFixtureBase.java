package nl.gmt.data;

import nl.gmt.data.test.TestConnection;
import org.apache.commons.lang.Validate;

public abstract class DbConnectionFixtureBase {
    protected TestConnection openDb() throws Exception {
        return openDb(createConfiguration());
    }

    protected TestConnection openDb(DbConfiguration cfg) throws Exception {
        Validate.notNull(cfg, "cfg");

        TestConnection db = new TestConnection(cfg);

        db.migrateDatabase();

        return db;
    }

    protected DbConfiguration createConfiguration() {
        DbConfiguration cfg = new DbConfiguration();

        cfg.setConnectionString("jdbc:postgresql://attissrv02/nhtest?user=nhtest&password=w92Nbz3curpXxXuK&currentSchema=public");
        cfg.setType(DbType.POSTGRES);

        return cfg;
    }
}
