package nl.gmt.data.multiTenancy;

import nl.gmt.data.DataException;
import nl.gmt.data.DbConfiguration;
import nl.gmt.data.DbTenant;
import nl.gmt.data.DbType;
import nl.gmt.data.migrate.SchemaMigrateException;
import nl.gmt.data.test.TestConnection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.sql.SQLException;

@RunWith(JUnit4.class)
public class MultiTenancyFixture {
    @Test
    public void simpleTest() throws DataException, SQLException, SchemaMigrateException {
        try (TestConnection connection = openConnection()) {
            connection.openContext(new Tenant("nhtest")).close();
        }
    }

    private TestConnection openConnection() throws DataException, SQLException, SchemaMigrateException {
        DbConfiguration cfg = new DbConfiguration();

        cfg.setConnectionString("jdbc:mysql://localhost/information_schema?user=nhtest&password=w92Nbz3curpXxXuK");
        cfg.setType(DbType.MYSQL);
        cfg.setEnableMultiTenancy(true);

        return new TestConnection(cfg);
    }

    private static class Tenant implements DbTenant {
        private final String database;

        private Tenant(String database) {
            this.database = database;
        }

        @Override
        public String getDatabase() {
            return database;
        }
    }
}
