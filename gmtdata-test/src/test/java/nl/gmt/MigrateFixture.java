package nl.gmt;

import nl.gmt.data.DataException;
import nl.gmt.data.DbConfiguration;
import nl.gmt.data.DbType;
import nl.gmt.data.drivers.SQLiteDriver;
import nl.gmt.data.migrate.SchemaMigrateException;
import nl.gmt.data.schema.SchemaException;
import nl.gmt.data.superModule.DbConnection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.sql.SQLException;

@RunWith(JUnit4.class)
public class MigrateFixture {
    @Test
    public void execute() throws SchemaException, DataException, SQLException, SchemaMigrateException {
        String path = "./tmp/test.db3";

        new File(path).getParentFile().mkdirs();

        SQLiteDriver.setPragma("journal_mode", "TRUNCATE");

        DbConfiguration cfg = new DbConfiguration();
        cfg.setConnectionString("jdbc:sqlite:" + path);
        cfg.setType(DbType.SQLITE);

        DbConnection db = new DbConnection(cfg);

        db.migrateDatabase();
    }
}
