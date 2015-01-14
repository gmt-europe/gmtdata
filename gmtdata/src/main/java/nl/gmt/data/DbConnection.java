package nl.gmt.data;

import nl.gmt.data.drivers.*;
import nl.gmt.data.migrate.*;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaCallback;
import nl.gmt.data.schema.SchemaClass;
import nl.gmt.data.schema.SchemaParserExecutor;
import nl.gmt.data.support.Delegate;
import nl.gmt.data.support.DelegateListener;
import org.apache.commons.lang3.Validate;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class DbConnection<T extends EntitySchema> implements DataCloseable {
    private final String connectionString;
    private final DbType type;
    private final String schemaName;
    private final Schema schema;
    private final DatabaseDriver driver;
    private SessionFactory sessionFactory;
    private final Delegate<DbContextTransition> contextTransitioned = new Delegate<>();
    private final RepositoryService repositoryService;
    private final T entitySchema;
    private boolean closed;

    protected DbConnection(String connectionString, DbType type, String schemaName) throws DataException {
        this(connectionString, type, schemaName, null);
    }

    protected DbConnection(String connectionString, DbType type, String schemaName, RepositoryService repositoryService) throws DataException {
        this.connectionString = connectionString;
        this.type = type;
        this.schemaName = schemaName;
        this.repositoryService = repositoryService;

        switch (type) {
            case SQLITE: driver = new SQLiteDatabaseDriver(); break;
            case MYSQL: driver = new MySqlDatabaseDriver(); break;
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

        driver.createConfiguration(configuration);

        createConfiguration(configuration);

        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
            .applySettings(configuration.getProperties())
            .build();

        sessionFactory = configuration.buildSessionFactory(serviceRegistry);

        driver.configure(this);

        entitySchema = createEntitySchema(schema);

        for (EntityType entityType : entitySchema.getEntityTypes()) {
            for (EntityField field : entityType.getFields()) {
                field.resolve(entitySchema);
            }
        }
    }

    protected abstract T createEntitySchema(Schema schema) throws DataException;

    protected void createConfiguration(Configuration configuration) {

    }

    public DbType getType() {
        return type;
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public T getEntitySchema() {
        return entitySchema;
    }

    private void addClasses(Configuration configuration) throws ClassNotFoundException {
        String ns = schema.getNamespace() + ".model";

        for (SchemaClass klass : schema.getClasses().values()) {
            String className = ns + ".";

            if (klass.getBoundedContext() != null)
                className += klass.getBoundedContext() + ".";

            className += klass.getName();

            configuration.addAnnotatedClass(Class.forName(className));
        }
    }

    @Override
    public void close() {
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
        if (isClosed()) {
            throw new IllegalStateException("Connection has already been closed");
        }

        return new DbContext(this);
    }

    public boolean isClosed() {
        return closed;
    }

    SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void addContextTransitioned(DelegateListener<DbContextTransition> listener) {
        contextTransitioned.add(listener);
    }

    public boolean removeContextTransitioned(DelegateListener<DbContextTransition> listener) {
        return contextTransitioned.remove(listener);
    }

    void raiseContextTransitioned(DbContext context, DbContextTransition transition) {
        Validate.notNull(context, "context");
        Validate.notNull(transition, "transition");

        contextTransitioned.call(context, transition);
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
