package nl.gmt.data;

import nl.gmt.data.drivers.SQLiteDriver;
import nl.gmt.data.test.TestConnection;

import java.io.File;

public abstract class DbConnectionFixtureBase {
    protected TestConnection openDb() throws Exception {
        String path = "./tmp/test.db3";

        new File(path).getParentFile().mkdirs();

        SQLiteDriver.setPragma("journal_mode", "TRUNCATE");

        DbConfiguration cfg = new DbConfiguration();
        cfg.setConnectionString("jdbc:sqlite:" + path);
        cfg.setType(DbType.SQLITE);

        TestConnection db = new TestConnection(cfg);

        db.migrateDatabase();

        return db;
    }
}
