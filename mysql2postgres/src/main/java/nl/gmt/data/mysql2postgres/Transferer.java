package nl.gmt.data.mysql2postgres;

import nl.gmt.data.DataException;
import nl.gmt.data.drivers.DatabaseDriver;
import nl.gmt.data.drivers.MySqlDatabaseDriver;
import nl.gmt.data.drivers.PostgresDatabaseDriver;
import nl.gmt.data.migrate.*;
import nl.gmt.data.schema.*;
import org.hibernate.internal.util.BytesHelper;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Transferer {
    private final Arguments arguments;
    private Connection from;
    private Connection to;
    private final MySqlDatabaseDriver fromDriver = new MySqlDatabaseDriver();
    private final PostgresDatabaseDriver toDriver = new PostgresDatabaseDriver();
    private Schema schema;

    public Transferer(Arguments arguments) {
        this.arguments = arguments;
    }

    public void transfer() throws DataException, SchemaException, SQLException, SchemaMigrateException {
        System.out.println("Connecting to the source database");

        from = fromDriver.createConnection(arguments.getFrom());
        from.setAutoCommit(false);

        System.out.println("Connecting to the destination database");

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

    private void transfer(SchemaClass klass) throws SQLException {
        System.out.print(String.format("%s...", klass.getName()));

        int count = getRecordCount(klass);
        int offset = 0;

        StringBuilder insert = new StringBuilder();
        StringBuilder select = new StringBuilder();

        insert.append("INSERT INTO \"").append(klass.getResolvedDbName()).append("\" (\"Id\"");
        select.append("SELECT `Id`");

        List<SchemaField> schemaFields = getSchemaFields(klass);

        for (SchemaField field : schemaFields) {
            if (field instanceof SchemaForeignChild) {
                continue;
            }

            String fieldName;
            if (field instanceof SchemaForeignParent) {
                fieldName = ((SchemaForeignParent)field).getResolvedDbName();
            } else {
                fieldName = ((SchemaProperty)field).getResolvedDbName();
            }

            insert.append(", \"").append(fieldName).append('"');
            select.append(", `").append(fieldName).append('`');
        }

        insert.append(") VALUES (?");

        for (int i = 0; i < schemaFields.size(); i++) {
            insert.append(", ?");
        }

        insert.append(')');
        select.append(" FROM `").append(klass.getResolvedDbName()).append("`");

        boolean[] uuidFields = getUuidFields(schemaFields);

        PreparedStatement toStmt = to.prepareStatement(insert.toString());

        try (Statement fromStmt = from.createStatement()) {
            ResultSet rs = fromStmt.executeQuery(select.toString());
            int fieldCount = schemaFields.size() + 1;

            while (rs.next()) {
                if (offset++ % 1000 == 0) {
                    to.commit();
                    System.out.print(String.format("\r%s %d of %d", klass.getName(), offset, count));
                }

                for (int i = 0; i < fieldCount; i++) {
                    Object value = rs.getObject(i + 1);
                    if (uuidFields[i]) {
                        value = bytesToUuid((byte[])value);
                    }
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

    private UUID bytesToUuid(byte[] value) {
        if (value == null) {
            return null;
        }

        byte[] msb = new byte[8];
        byte[] lsb = new byte[8];
        System.arraycopy(value, 0, msb, 0, 8);
        System.arraycopy(value, 8, lsb, 0, 8);
        return new UUID(BytesHelper.asLong(msb), BytesHelper.asLong(lsb));
    }

    private boolean[] getUuidFields(List<SchemaField> fields) {
        boolean[] uuidFields = new boolean[fields.size() + 1];
        boolean idIsUuid = schema.getIdProperty().getResolvedDataType().getNativeType() == UUID.class;
        int offset = 1;

        uuidFields[0] = idIsUuid;

        for (SchemaField field : fields) {
            if (field instanceof SchemaForeignChild) {
                continue;
            }

            if (field instanceof SchemaProperty) {
                uuidFields[offset] = ((SchemaProperty)field).getResolvedDataType().getNativeType() == UUID.class;
            } else {
                uuidFields[offset] = idIsUuid;
            }

            offset++;
        }

        return uuidFields;
    }

    private int getRecordCount(SchemaClass klass) throws SQLException {
        try (Statement stmt = from.createStatement()) {
            ResultSet rs = stmt.executeQuery(String.format("SELECT COUNT(*) FROM `%s`", klass.getResolvedDbName()));

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
