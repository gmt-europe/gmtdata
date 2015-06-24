package nl.gmt.data.migrate;

import nl.gmt.data.DataException;
import nl.gmt.data.drivers.DatabaseDriver;
import nl.gmt.data.schema.SchemaCallback;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class DatabaseTestBase {
    private Connection connection;

    protected Connection getConnection() {
        return connection;
    }

    protected abstract DatabaseDriver createDatabaseDriver();

    protected abstract String getConnectionStringName();

    protected abstract Connection createConnection(String connectionString) throws DataException;

    @Before
    public void SetUp() throws Exception {
        if (connection != null)
            connection.close();

        connection = createConnection(getConnectionString());

        resetDatabase();
    }

    private void resetDatabase() throws Exception {
        // Migrate to the empty database to reset the database.

        execute("", true, ExpectChanges.DONT_CARE);
    }

    @After
    public void tearDown() throws SQLException {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    protected void execute(String testScript) {
        execute(testScript, ExpectChanges.YES);
    }

    protected void execute(String testScript, ExpectChanges expectChanges) {
        execute(testScript, false, expectChanges);
    }

    protected void execute(String testScript, boolean suppressConsole, ExpectChanges expectChanges) {
        if (!suppressConsole) {
            System.out.println("Applying schema");
            System.out.println();
        }

        // Execute the schema and apply the statements.

        SchemaCallbackImpl callback = new SchemaCallbackImpl(testScript);

        DataSchemaExecutor executor = new DataSchemaExecutor(
            new DataSchemaExecutorConfiguration(
                SchemaCallbackImpl.SCHEMA_NAME,
                getConnectionString(),
                false,
                createDatabaseDriver()
            ),
            callback
        );

        try {
            executor.execute();
        } catch (SchemaMigrateException e) {
            throw new RuntimeException(e);
        }

        boolean hadChanges = callbackHasChanges(callback);

        switch (expectChanges)
        {
            case YES:
                if (!hadChanges)
                    throw new RuntimeException("Expected changes but none were generated");
                break;

            case NO:
                if (hadChanges)
                    throw new RuntimeException("Did not expect changes, but changes were generated");
                break;
        }

        for (SqlStatement statement : callback.statements) {
            if (statement.getType() == SqlStatementType.STATEMENT) {
                if (!suppressConsole) {
                    System.out.println(StringUtils.stripEnd(statement.getValue(), null));
                    System.out.println();
                }

                try {
                    executeCommand(statement.getValue());
                } catch (SQLException e) {
                    if (expectChanges == ExpectChanges.THROWS) {
                        return;
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        if (expectChanges == ExpectChanges.THROWS) {
            throw new RuntimeException("Expected the migration to throw");
        }

        // Execute the schema again to verify that the generator sees
        // the changes have been correctly applied.

        callback = new SchemaCallbackImpl(testScript);

        executor = new DataSchemaExecutor(
            new DataSchemaExecutorConfiguration(
                SchemaCallbackImpl.SCHEMA_NAME,
                getConnectionString(),
                false,
                createDatabaseDriver()
            ),
            callback
        );

        try {
            executor.execute();
        } catch (SchemaMigrateException e) {
            throw new RuntimeException(e);
        }

        if (callbackHasChanges(callback)) {
            StringBuilder sb = new StringBuilder();
            int count = 0;

            for (SqlStatement statement : callback.statements) {
                if (statement.getType() == SqlStatementType.STATEMENT)
                    count++;

                sb.append(statement.getValue());
                sb.append("\n");
            }

            throw new RuntimeException(String.format(
                "Test failed; second pass resulted in %d statements\r\n\r\n%s",
                count,
                sb.toString()
            ));
        }
    }

    private boolean callbackHasChanges(SchemaCallbackImpl callback) {
        boolean hadChanges = false;

        for (SqlStatement statement : callback.statements) {
            if (statement.getType() == SqlStatementType.STATEMENT) {
                hadChanges = true;
                break;
            }
        }
        return hadChanges;
    }

    private void executeCommand(String sql, Object... args) throws SQLException {
        if (args != null && args.length > 0)
            sql = String.format(sql, args);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    private String getConnectionString() {
        try {
            try (InputStream stream = getClass().getResourceAsStream("Configuration.xml")) {
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);

                for (Node node = document.getFirstChild(); node != null; node = node.getNextSibling()) {
                    if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("connectionStrings")) {
                        for (Node childNode = node.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
                            if (childNode.getNodeType() == Node.ELEMENT_NODE && childNode.getNodeName().equals("connectionString")) {
                                if (childNode.getAttributes().getNamedItem("type").getNodeValue().equals(getConnectionStringName()))
                                    return childNode.getAttributes().getNamedItem("value").getNodeValue();
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException("Cannot find connection string " + getConnectionStringName(), e);
        }

        throw new RuntimeException("Cannot find connection string " + getConnectionStringName());
    }

    private static class SchemaCallbackImpl implements SchemaCallback {
        public static final String SCHEMA_NAME = "$Unit test schema.schema";

        private final String schema;
        private Iterable<SqlStatement> statements;

        public SchemaCallbackImpl(String schema) {
            this.schema = schema;
        }

        @Override
        public InputStream loadFile(String schema) throws Exception {
            if (schema.equals(SCHEMA_NAME))
                return new ByteArrayInputStream(this.schema.getBytes("UTF-8"));

            throw new RuntimeException("Cannot load schema");
        }

        @Override
        public void serializeSql(Iterable<SqlStatement> statements) {
            this.statements = statements;
        }
    }
}
