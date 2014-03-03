package nl.gmt.data.drivers;

import nl.gmt.data.DbConnection;
import nl.gmt.data.DbContext;
import nl.gmt.data.DbContextListener;
import nl.gmt.data.migrate.SchemaMigrateException;
import nl.gmt.data.migrate.SqlGenerator;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaRules;
import org.apache.commons.lang.Validate;
import org.hibernate.dialect.SQLiteDialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class SQLiteDatabaseDriver extends GenericDatabaseDriver {
    @Override
    public String getConnectionType() {
        return SQLiteDriver.class.getName();
    }

    @Override
    public String getDialectType() {
        return SQLiteDialect.class.getName();
    }

    @Override
    public SqlGenerator createSqlGenerator(Schema schema) throws SchemaMigrateException {
        return new nl.gmt.data.migrate.sqlite.SqlGenerator(schema);
    }

    @Override
    public SchemaRules createSchemaRules() throws SchemaMigrateException {
        return new nl.gmt.data.migrate.sqlite.SchemaRules();
    }

    @Override
    public void configure(DbConnection db) {
        // SQLite doesn't like concurrent access. It supports it, but when we're writing
        // from multiple threads, we may get an exception when a second thread tries to
        // start a transaction while the first thread is in a transaction. A simple solution
        // which gives very little issues is to synchronize all transactions, which this
        // listener does.

        db.addContextListener(new DbContextListenerImpl());
    }

    private static class DbContextListenerImpl implements DbContextListener {
        private final ReentrantLock lock = new ReentrantLock();

        @Override
        public void beforeOpenContext(DbContext context) {
            lock.lock();
        }

        @Override
        public void afterOpenContext(DbContext context) {
        }

        @Override
        public void beforeCloseContext(DbContext context) {
        }

        @Override
        public void afterCloseContext(DbContext context) {
            lock.unlock();
        }
    }
}
