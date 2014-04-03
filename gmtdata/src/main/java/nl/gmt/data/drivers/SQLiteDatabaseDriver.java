package nl.gmt.data.drivers;

import nl.gmt.DelegateListener;
import nl.gmt.data.DbConnection;
import nl.gmt.data.DbContextTransition;
import nl.gmt.data.migrate.SchemaMigrateException;
import nl.gmt.data.migrate.SqlGenerator;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaRules;
import org.hibernate.dialect.SQLiteDialect;

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
    @SuppressWarnings("unchecked")
    public void configure(DbConnection db) {
        // SQLite doesn't like concurrent access. It supports it, but when we're writing
        // from multiple threads, we may get an exception when a second thread tries to
        // start a transaction while the first thread is in a transaction. A simple solution
        // which gives very little issues is to synchronize all transactions, which this
        // listener does.

        db.addContextTransitioned(new DelegateListener<DbContextTransition>() {
            private final ReentrantLock lock = new ReentrantLock();
            private boolean taken;

            @Override
            public void call(Object sender, DbContextTransition transition) {
                switch (transition) {
                    case OPENING:
                        lock.lock();

                        // Prevent re-entrant locking.

                        if (taken) {
                            lock.unlock();

                            throw new IllegalStateException("Nested transactions are not supported");
                        }

                        taken = true;
                        break;

                    case CLOSED:
                        taken = false;

                        lock.unlock();
                        break;
                }
            }
        });
    }
}
