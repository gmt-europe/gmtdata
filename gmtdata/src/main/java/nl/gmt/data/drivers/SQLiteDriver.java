package nl.gmt.data.drivers;

import org.apache.commons.lang3.Validate;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class SQLiteDriver implements Driver {
    private static Map<String, String> PRAGMAS;

    private final Driver driver;
    private SQLiteDatabaseDriver databaseDriver;

    public SQLiteDriver() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        driver = (Driver)Class.forName("org.sqlite.JDBC").newInstance();
    }

    @Override
    public Connection connect(String s, Properties properties) throws SQLException {
        Connection connection = driver.connect(s, properties);

        initializeConnection(connection);

        return connection;
    }

    void initializeConnection(Connection connection) throws SQLException {
        // TODO: This is a very dirty construction. What we're doing here is that we're using a static map with
        // configuration to configure the SQLite connection. The problem here is that it's static and not associated
        // e.g. with the SQLiteDatabaseDriver, what it should be. The reason for this construction is that I could not
        // find a way to decently associated the SQLiteDatabaseDriver instance with this class. Suggestions are welcome.

        Map<String, String> pragmas = PRAGMAS;

        if (pragmas == null) {
            return;
        }

        for (Map.Entry<String, String> entry : pragmas.entrySet()) {
            String sql = "PRAGMA " + entry.getKey() + " = " + entry.getValue();

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.execute();
            }
        }
    }

    @Override
    public boolean acceptsURL(String s) throws SQLException {
        return driver.acceptsURL(s);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String s, Properties properties) throws SQLException {
        return driver.getPropertyInfo(s, properties);
    }

    @Override
    public int getMajorVersion() {
        return driver.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return driver.getMinorVersion();
    }

    @Override
    public boolean jdbcCompliant() {
        return driver.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return driver.getParentLogger();
    }

    public static void setPragma(String pragma, String value) {
        Validate.notNull(pragma, "pragma");
        Validate.notNull(value, "value");

        // We build a new hash set every time we add a value to not have to synchronize access to it.

        Map<String, String> pragmas = PRAGMAS;

        if (pragmas != null) {
            pragmas = new HashMap<>(pragmas);
        } else {
            pragmas = new HashMap<>();
        }

        if (value == null) {
            pragmas.remove(pragma);
        } else {
            pragmas.put(pragma, value);
        }

        if (pragmas.size() == 0) {
            PRAGMAS = null;
        } else {
            PRAGMAS = pragmas;
        }
    }

    public static String getPragma(String pragma) {
        Validate.notNull(pragma, "pragma");

        return PRAGMAS.get(pragma);
    }
}
