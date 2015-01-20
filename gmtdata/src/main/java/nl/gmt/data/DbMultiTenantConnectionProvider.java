package nl.gmt.data;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public class DbMultiTenantConnectionProvider implements MultiTenantConnectionProvider, Configurable, Stoppable, ServiceRegistryAwareService {
    private static final Logger LOG = LogManager.getLogger(DbMultiTenantConnectionProvider.class);

    private String defaultDatabase;
    private final C3P0ConnectionProvider connectionProvider = new C3P0ConnectionProvider();

    @Override
    public void configure(Map configurationValues) {
        connectionProvider.configure(configurationValues);

        try {
            Connection connection = getAnyConnection();

            try {
                defaultDatabase = connection.getCatalog();
            } finally {
                releaseAnyConnection(connection);
            }
        } catch (SQLException e) {
            LOG.error("Couldn't get default database name", e);

            throw new HibernateException(e);
        }
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return connectionProvider.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connectionProvider.closeConnection(connection);
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = getAnyConnection();

        try {
            connection.setCatalog(tenantIdentifier);
        } catch (SQLException e) {
            LOG.warn("Couldn't set catalog for tenant connection", e);

            throw e;
        }

        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try {
            connection.setCatalog(defaultDatabase);
        } catch (SQLException e) {
            // Do not return the connection to the connection provider when we couldn't switch back to the default
            // database.

            try {
                connection.close();
            } catch (SQLException e1) {
                LOG.warn("Exception when closing connection that couldn't be set to the default catalog", e);
            }

            LOG.error("Couldn't revert catalog to return it to the connection pool", e);

            throw e;
        }

        connectionProvider.closeConnection(connection);
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return connectionProvider.supportsAggressiveRelease();
    }

    @Override
    public void stop() {
        connectionProvider.stop();
    }

    @Override
    public boolean isUnwrappableAs(Class unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        throw new UnknownUnwrapTypeException(unwrapType);
    }

    @Override
    public void injectServices(ServiceRegistryImplementor serviceRegistry) {
        connectionProvider.injectServices(serviceRegistry);
    }
}
