package nl.gmt.data.drivers;

import nl.gmt.data.migrate.Manifest;
import nl.gmt.data.migrate.SchemaMigrateException;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaIdAutoIncrement;

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
            if (metaData.getColumnName(i) == "SchemaHash")
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
}
