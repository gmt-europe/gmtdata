package nl.gmt.data.migrator;

import nl.gmt.data.DataException;
import nl.gmt.data.drivers.DatabaseDriver;
import nl.gmt.data.drivers.MySqlDatabaseDriver;
import nl.gmt.data.drivers.PostgresDatabaseDriver;
import nl.gmt.data.drivers.SqlServerDatabaseDriver;
import nl.gmt.data.migrate.*;
import nl.gmt.data.schema.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Transferer {
    private final Arguments arguments;
    private Connection from;
    private Connection to;
    private DatabaseDriver fromDriver;
    private DatabaseDriver toDriver;
    private Schema schema;

    public Transferer(Arguments arguments) {
        this.arguments = arguments;
    }

    public void transfer() throws DataException, SchemaException, SQLException, SchemaMigrateException {
        System.out.println("Connecting to the source database");

        fromDriver = createDriver(arguments.getFrom());
        from = fromDriver.createConnection(arguments.getFrom());
        from.setAutoCommit(false);

        System.out.println("Connecting to the destination database");

        toDriver = createDriver(arguments.getTo());
        to = toDriver.createConnection(arguments.getTo());
        to.setAutoCommit(false);

        System.out.println("Loading the schema");

        SchemaParserExecutor parserExecutor = new SchemaParserExecutor(new SchemaCallbackImpl());
        schema = parserExecutor.parse(arguments.getSchema(), GenericSchemaRules.INSTANCE);

        System.out.println("Clearing the target database");

        migrate(arguments.getTo(), "Empty.schema", toDriver, false);

        System.out.println("Creating the target database without constraints or indexes");

        migrate(arguments.getTo(), arguments.getSchema(), toDriver, true);

        System.out.println("Transferring data...");

        for (Map.Entry<String, SchemaClass> entry : schema.getClasses().entrySet()) {
            transfer(entry.getValue());
        }

        System.out.println("Creating constraints and indexes");

        migrate(arguments.getTo(), arguments.getSchema(), toDriver, false);
    }

    private DatabaseDriver createDriver(String connectionString) {
        if (connectionString.startsWith("jdbc:postgresql:")) {
            return new PostgresDatabaseDriver();
        }
        if (connectionString.startsWith("jdbc:mysql:")) {
            return new MySqlDatabaseDriver();
        }
        if (connectionString.startsWith("jdbc:sqlserver:")) {
            return new SqlServerDatabaseDriver();
        }
        throw new IllegalStateException("Cannot resolve database driver from the connection string");
    }

    private void transfer(SchemaClass klass) throws SQLException {
        DbLoader sourceLoader = DbLoader.fromDriver(fromDriver, schema, klass);
        DbLoader targetLoader = DbLoader.fromDriver(toDriver, schema, klass);

        System.out.print(String.format("%s...", klass.getName()));

        int count = getRecordCount(sourceLoader.buildCountQuery());
        int offset = 0;

        String selectQuery = sourceLoader.buildSelectQuery();
        String insertQuery = targetLoader.buildInsertQuery();

        List<SchemaField> schemaFields = getSchemaFields(klass);

        try (
            PreparedStatement toStmt = to.prepareStatement(insertQuery);
            Statement fromStmt = from.createStatement()
        ) {
            ResultSet rs = fromStmt.executeQuery(selectQuery);
            int fieldCount = schemaFields.size() + 1;

            while (rs.next()) {
                if (offset++ % 1000 == 0) {
                    to.commit();
                    System.out.print(String.format("\r%s %d of %d", klass.getName(), offset, count));
                }

                for (int i = 0; i < fieldCount; i++) {
                    Object value = rs.getObject(i + 1);
                    value = targetLoader.printValue(i, sourceLoader.parseValue(i, value));
                    toStmt.setObject(i + 1, value);
                }

                toStmt.executeUpdate();
            }

            to.commit();
            from.commit();
        }

        System.out.println();
    }

    private List<SchemaField> getSchemaFields(SchemaClass klass) {
        List<SchemaField> fields = new ArrayList<>();

        addSchemaFields(klass, fields);

        return fields;
    }

    private void addSchemaFields(SchemaClassBase klass, List<SchemaField> fields) {
        for (SchemaField field : klass.getFields()) {
            if (!(field instanceof SchemaForeignChild)) {
                fields.add(field);
            }
        }

        for (String mixin : klass.getMixins()) {
            addSchemaFields(schema.getMixins().get(mixin), fields);
        }
    }

    private int getRecordCount(String query) throws SQLException {
        try (Statement stmt = from.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);

            if (rs.next()) {
                return rs.getInt(1);
            }

            throw new IllegalStateException("Cannot get record count");
        }
    }

    private void migrate(String connectionString, String schemaName, DatabaseDriver driver, boolean noConstraintsOrIndexes) throws SchemaMigrateException, DataException, SQLException {
        SchemaCallbackImpl callback = new SchemaCallbackImpl();

        DataSchemaExecutor executor = new DataSchemaExecutor(
            new DataSchemaExecutorConfiguration(
                schemaName,
                connectionString,
                noConstraintsOrIndexes,
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

    private class SchemaCallbackImpl implements SchemaCallback {
        private Iterable<SqlStatement> statements;

        @Override
        public InputStream loadFile(String schema) throws Exception {
            InputStream stream = getClass().getResourceAsStream(schema);
            if (stream != null) {
                return stream;
            }

            return new FileInputStream(schema);
        }

        @Override
        public void serializeSql(Iterable<SqlStatement> statements) {
            this.statements = statements;
        }
    }
}
