package nl.gmt.data;

import nl.gmt.data.drivers.DatabaseDriver;
import nl.gmt.data.drivers.MySqlDatabaseDriver;
import nl.gmt.data.drivers.PostgresDatabaseDriver;
import nl.gmt.data.drivers.SQLiteDatabaseDriver;
import nl.gmt.data.migrate.*;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaCallback;
import nl.gmt.data.schema.SchemaParserExecutor;
import nl.gmt.data.support.Delegate;
import nl.gmt.data.support.DelegateListener;
import nl.gmt.data.support.UTF8Control;
import org.apache.commons.lang.Validate;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.common.reflection.MetadataProviderInjector;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;

public abstract class DbConnection<T extends EntitySchema> implements DataCloseable {
    private static final ResourceBundle BUNDLE = loadBundle();

    private static ResourceBundle loadBundle() {
        return ResourceBundle.getBundle(DbConnection.class.getPackage().getName() + ".Messages", new UTF8Control());
    }

    private final ThreadLocal<DbContext> currentContext = new ThreadLocal<>();
    private final String connectionString;
    private final DbType type;
    private final String schemaName;
    private final DatabaseDriver driver;
    private SessionFactory sessionFactory;
    private final Delegate<DbContextTransition> contextTransitioned = new Delegate<>();
    private final RepositoryService repositoryService;
    private final T entitySchema;
    private final DbEntityUsageManager<T> usageManager;
    private final DbConfiguration.OnResolveMessage messageResolver;
    private boolean closed;

    @SuppressWarnings("unchecked")
    protected DbConnection(DbConfiguration configuration, String schemaName, RepositoryService repositoryService) throws DataException {
        Validate.notNull(configuration, "configuration");
        Validate.notNull(schemaName, "schemaName");

        this.connectionString = configuration.getConnectionString();
        this.type = configuration.getType();
        this.schemaName = schemaName;
        this.repositoryService = repositoryService;
        this.messageResolver = configuration.getMessageResolver();

        switch (type) {
            case SQLITE: driver = new SQLiteDatabaseDriver(); break;
            case MYSQL: driver = new MySqlDatabaseDriver(); break;
            case POSTGRES: driver = new PostgresDatabaseDriver(); break;
            default: throw new DataException("Illegal database type");
        }

        SchemaParserExecutor parserExecutor = new SchemaParserExecutor(new SchemaCallbackImpl());

        Configuration cfg = new Configuration()
            .setProperty("hibernate.dialect", driver.getDialectType())
            .setProperty("hibernate.connection.url", connectionString)
            .setProperty("hibernate.connection.driver_class", driver.getConnectionType());

        // The standard UUID type does not work for Postgres. The problem is that the standard
        // UUID type converts from/to byte array or string. The Postgres driver actually accepts
        // proper UUID instances so does not need this conversion. To use the correct provider,
        // an `@Type(type = "pg-uuid")` annotation needs to be added to the generated source. However,
        // this is not compatible with other databases. To work around this, we insert these
        // annotations at runtime only when the database is Postgres.

        if (type == DbType.POSTGRES) {
            MetadataProviderInjector reflectionManager = (MetadataProviderInjector)cfg.getReflectionManager();
            reflectionManager.setMetadataProvider(new UUIDTypeInsertingMetadataProvider(reflectionManager.getMetadataProvider()));
        }

        if (configuration.isEnableMultiTenancy()) {
            cfg.setProperty("hibernate.multi_tenant_connection_provider", DbMultiTenantConnectionProvider.class.getName());
            cfg.setProperty("hibernate.multiTenancy", "SCHEMA");
        }

        Schema schema;

        try {
            schema = parserExecutor.parse(schemaName, driver.createSchemaRules());
        } catch (Throwable e) {
            throw new DataException("Cannot load schema", e);
        }

        entitySchema = createEntitySchema(schema);

        addClasses(cfg);

        driver.createConfiguration(cfg);

        createConfiguration(cfg);

        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
            .applySettings(cfg.getProperties())
            .build();

        sessionFactory = cfg.buildSessionFactory(serviceRegistry);

        driver.configure(this);

        for (EntityType entityType : entitySchema.getEntityTypes()) {
            for (EntityField field : entityType.getFields()) {
                field.resolve(entitySchema);
            }
        }

        usageManager = new DbEntityUsageManager(this);
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

    private void addClasses(Configuration configuration) {
        for (EntityType entityType : entitySchema.getEntityTypes()) {
            configuration.addAnnotatedClass(entityType.getModel());
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
        migrateDatabase(null);
    }

    public void migrateDatabase(DbTenant tenant) throws DataException, SchemaMigrateException, SQLException {
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
            if (tenant != null) {
                connection.setCatalog(tenant.getDatabase());
            }

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
        return openContext(null);
    }

    public DbContext openContext(DbTenant tenant) {
        if (isClosed()) {
            throw new IllegalStateException("Connection has already been closed");
        }

        return new DbContext(this, tenant);
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

    void setCurrentContext(DbContext context) throws DataException {
        if (currentContext.get() != null) {
            throw new DataException("Cannot open multiple contexts");
        }

        currentContext.set(context);
    }

    void clearCurrentContext() {
        assert currentContext.get() != null;

        currentContext.remove();
    }

    public DbContext getCurrentContext() {
        return currentContext.get();
    }

    public DbEntityUsage getUsage(DbContext ctx, Entity entity, EntityType... exclusions) {
        return usageManager.getUsage(ctx, entity, exclusions);
    }

    String getText(String key, Object... args) {
        Validate.notNull(key, "key");

        String result = null;

        if (messageResolver != null) {
            result = messageResolver.onResolveMessage(key);
        }

        if (result == null) {
            result = BUNDLE.getString(key);
        }

        if (args != null && args.length > 0) {
            result = String.format(result, args);
        }

        return result;
    }

    private class SchemaCallbackImpl implements SchemaCallback {
        private Iterable<SqlStatement> statements;

        @Override
        public InputStream loadFile(String schema) throws Exception {
            // First try the schema name as is. If that doesn't resolve, try resolving it as an absolute path.

            InputStream stream = DbConnection.this.getClass().getResourceAsStream(schema);
            if (stream != null) {
                return stream;
            }
            return DbConnection.this.getClass().getResourceAsStream("/" + schema);
        }

        @Override
        public void serializeSql(Iterable<SqlStatement> statements) {
            this.statements = statements;
        }
    }
}
