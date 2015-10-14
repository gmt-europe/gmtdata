package nl.gmt.data.drivers;

import nl.gmt.data.DbConfiguration;
import nl.gmt.data.migrate.Manifest;
import nl.gmt.data.migrate.SchemaMigrateException;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaIdAutoIncrement;
import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.cfg.Configuration;

import java.sql.*;
import java.util.UUID;

public abstract class GenericDatabaseDriver extends DatabaseDriver {
    @Override
    public Manifest readManifest(Connection connection) throws SchemaMigrateException {
        try (Statement stmt = connection.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT * FROM GmtMigrate")) {
                while (rs.next()) {
                    return new Manifest(
                        rs.getString("Version"),
                        getSchemaHash(rs)
                    );
                }
            }
        } catch (SQLException e) {
            throw new SchemaMigrateException("Cannot read manifest", e);
        }

        return null;
    }

    private String getSchemaHash(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();

        for (int i = 0; i < metaData.getColumnCount(); i++) {
            if (metaData.getColumnName(i).equals("SchemaHash"))
                return rs.getString(i);
        }

        return null;
    }

    @Override
    public void writeManifest(Connection connection, Manifest manifest, Schema schema) throws SchemaMigrateException {
        try {
            String sql;
            boolean generateId = false;

            if (readManifest(connection) != null) {
                sql = "UPDATE GmtMigrate SET Version = ?, SchemaHash = ?";
            } else if (schema.getIdProperty().getAutoIncrement() == SchemaIdAutoIncrement.YES) {
                sql = "INSERT INTO GmtMigrate (Version, SchemaHash) VALUES (?, ?)";
            } else {
                sql = "INSERT INTO GmtMigrate (Version, SchemaHash, " + schema.getIdProperty().getName() + ") VALUES (?, ?, ?)";
                generateId = true;
            }

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, manifest.getVersion());
                stmt.setString(2, manifest.getSchemaHash());

                if (generateId) {
                    if (schema.getIdProperty().getResolvedDataType() == null)
                        throw new SchemaMigrateException("Missing ID property type");

                    Class<?> nativeType = schema.getIdProperty().getResolvedDataType().getNativeType();

                    if (nativeType == UUID.class)
                        stmt.setObject(3, UUID.randomUUID());
                    else if (nativeType == Integer.class)
                        stmt.setInt(3, 1);
                    else
                        throw new SchemaMigrateException(String.format("Cannot generate ID for '%s'", nativeType));
                }

                stmt.executeUpdate();

                connection.commit();
            }
        } catch (SQLException e) {
            throw new SchemaMigrateException("Cannot write manifest", e);
        }
    }

    protected void configureConnectionPooling(Configuration cfg, DbConfiguration configuration, String preferredTestQuery) {
        int minSize = configuration.getConnectionPoolMinSize();
        if (minSize == -1) {
            minSize = 0;
        }
        int maxSize = configuration.getConnectionPoolMaxSize();
        if (maxSize == -1) {
            maxSize = 15;
        }

        cfg
            .setProperty("connection.provider_class", C3P0ConnectionProvider.class.getName())
            .setProperty("hibernate.c3p0.acquire_increment", "3")
            .setProperty("hibernate.c3p0.idle_test_period", "14400")
            .setProperty("hibernate.c3p0.min_size", Integer.toString(minSize))
            .setProperty("hibernate.c3p0.max_size", Integer.toString(maxSize))
            .setProperty("hibernate.c3p0.max_statements", "0")
            .setProperty("hibernate.c3p0.timeout", "25200")
            .setProperty("hibernate.c3p0.preferredTestQuery", preferredTestQuery);
    }
}
