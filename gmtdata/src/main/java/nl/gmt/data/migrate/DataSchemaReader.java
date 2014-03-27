package nl.gmt.data.migrate;

import java.sql.Connection;
import java.util.Map;

public abstract class DataSchemaReader {
    private final Connection connection;

    protected DataSchemaReader(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }

    public abstract Map<String, DataSchemaTable> getTables() throws SchemaMigrateException;
}
