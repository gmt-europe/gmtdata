package nl.gmt.data;

import nl.gmt.data.drivers.SQLiteDriver;
import nl.gmt.data.test.TestConnection;
import org.apache.commons.lang.Validate;

import java.io.File;

public abstract class DbConnectionFixtureBase {
    protected TestConnection openDb() throws Exception {
        return openDb(createConfiguration());
    }

    protected TestConnection openDb(DbConfiguration cfg) throws Exception {
        Validate.notNull(cfg, "cfg");

        SQLiteDriver.setPragma("journal_mode", "TRUNCATE");

        TestConnection db = new TestConnection(cfg);

        db.migrateDatabase();

        return db;
    }

    protected DbConfiguration createConfiguration() {
        String path = "./tmp/test.db3";

        new File(path).getParentFile().mkdirs();

        DbConfiguration cfg = new DbConfiguration();
        cfg.setConnectionString("jdbc:sqlite:" + path);
        cfg.setType(DbType.SQLITE);
        return cfg;
    }
}
