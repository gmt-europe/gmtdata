package nl.gmt.data.drivers;

import nl.gmt.data.DataException;
import nl.gmt.data.DbConfiguration;
import nl.gmt.data.DbConnection;
import nl.gmt.data.migrate.Manifest;
import nl.gmt.data.migrate.SchemaMigrateException;
import nl.gmt.data.migrate.SqlGenerator;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaRules;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public abstract class DatabaseDriver {
    public abstract String getConnectionType();

    public abstract Manifest readManifest(Connection connection) throws SchemaMigrateException;

    public abstract void writeManifest(Connection connection, Manifest manifest, Schema schema) throws SchemaMigrateException;

    public abstract String getDialectType();

    public void configure(DbConnection db) {
    }

    public Connection createConnection(String connectionString) throws DataException {
        try {
            Class.forName(getConnectionType());

            return DriverManager.getConnection(connectionString);
        } catch (Throwable e) {
            throw new DataException("Cannot open connection", e);
        }
    }

    public void databasePerform(String connectionString, ConnectionAction action) throws DataException {
        try (Connection connection = createConnection(connectionString)) {
            boolean success = false;

            try {
                action.action(connection);

                success = true;
            } catch (Throwable e) {
                throw new DataException("Failed to execute database action", e);
            } finally {
                if (success) {
                    connection.commit();
                } else {
                    connection.rollback();
                }
            }
        } catch (SQLException e) {
            throw new DataException("Cannot execute database action", e);
        }
    }

    public SqlGenerator createSqlGenerator(Schema schema) throws SchemaMigrateException {
        throw new SchemaMigrateException("SQL generation not supported");
    }

    public SchemaRules createSchemaRules() throws SchemaMigrateException {
        throw new SchemaMigrateException("SQL generation not supported");
    }

    public void createConfiguration(StandardServiceRegistryBuilder serviceRegistryBuilder, DbConfiguration configuration) {
        
    }

    public static interface ConnectionAction {
        void action(Connection connection) throws Throwable;
    }
}
