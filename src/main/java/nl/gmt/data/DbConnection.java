package nl.gmt.data;

import nl.gmt.data.drivers.DatabaseDriver;
import nl.gmt.data.drivers.SQLiteDatabaseDriver;
import nl.gmt.data.migrate.*;
import nl.gmt.data.schema.*;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class DbConnection implements AutoCloseable {
    private final String connectionString;
    private final DbType type;
    private final String schemaName;
    private final Schema schema;
    private final DatabaseDriver driver;
    private SessionFactory sessionFactory;
    private final StandardServiceRegistry serviceRegistry;
    private List<DbContextListener> contextListeners = new ArrayList<>();
    private List<DbContextListener> unmodifiableContextListeners = Collections.unmodifiableList(contextListeners);
    private boolean closed;

    protected DbConnection(String connectionString, DbType type, String schemaName) throws DataException {
        this.connectionString = connectionString;
        this.type = type;
        this.schemaName = schemaName;

        switch (type) {
            case SQLITE: driver = new SQLiteDatabaseDriver(); break;
            default: throw new DataException("Illegal database type");
        }

        SchemaParserExecutor parserExecutor = new SchemaParserExecutor(new SchemaCallbackImpl());

        Configuration configuration= new Configuration()
            .setProperty("hibernate.dialect", driver.getDialectType())
            .setProperty("hibernate.connection.url", connectionString)
            .setProperty("hibernate.connection.driver_class", driver.getConnectionType());

        try {
            schema = parserExecutor.parse(schemaName, driver.createSchemaRules());

            addClasses(configuration);
        } catch (Throwable e) {
            throw new DataException("Cannot load schema", e);
        }

        serviceRegistry = new StandardServiceRegistryBuilder()
            .applySettings(configuration.getProperties())
            .build();

        sessionFactory = configuration.buildSessionFactory(serviceRegistry);

        driver.configure(this);
    }

    private void addClasses(Configuration configuration) throws ClassNotFoundException {
        String ns = schema.getNamespace() + ".model";

        for (SchemaClass klass : schema.getClasses().values()) {
            String className = ns + ".";

            if (klass.getBoundedContext() != null)
                className += "model.";

            className += klass.getName();

            configuration.addAnnotatedClass(Class.forName(className));
        }
    }

    @Override
    public void close() throws Exception {
        if (!closed) {
            if (sessionFactory != null) {
                sessionFactory.close();
                sessionFactory = null;
            }

            closed = true;
        }
    }

    public void migrateDatabase() throws DataException, SchemaMigrateException, SQLException {
        SchemaCallbackImpl callback = new SchemaCallbackImpl();

        DataSchemaExecutor executor = new DataSchemaExecutor(
            new DataSchemaExecutorConfiguration(
                schemaName,
                connectionString,
                false,
                driver
            ),
            callback
        );

        executor.execute();

        try (Connection connection = driver.createConnection(connectionString)) {
            for (SqlStatement statement : callback.statements) {
                if (statement.getType() == SqlStatementType.STATEMENT) {
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(statement.getValue());
                    }
                }
            }
        }
    }

    public DbContext openContext() {
        return new DbContext(this);
    }

    public boolean isClosed() {
        return closed;
    }

    SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    List<DbContextListener> getContextListeners() {
        return unmodifiableContextListeners;
    }

    public void addContextListener(DbContextListener listener) {
        contextListeners.add(listener);
    }

    private class SchemaCallbackImpl implements SchemaCallback {
        private Iterable<SqlStatement> statements;

        @Override
        public InputStream loadFile(String schema) throws Exception {
            return DbConnection.this.getClass().getResourceAsStream(schema);
        }

        @Override
        public void serializeSql(Iterable<SqlStatement> statements) {
            this.statements = statements;
        }
    }
}
