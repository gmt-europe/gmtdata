package nl.gmt.data.migrate.mysql;

import nl.gmt.data.DataException;
import nl.gmt.data.migrate.*;
import nl.gmt.data.schema.Schema;
import org.apache.commons.lang.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class SqlGenerator extends GuidedSqlGenerator {
    private SchemaRules rules;
    private boolean closed;
    private Connection connection;
    private Map<String, CharacterSet> characterSets;
    private Map<String, Collation> collations;
    private Map<String, StorageEngine> storageEngines;

    public SqlGenerator(Schema schema) {
        super(schema);
    }

    @Override
    protected nl.gmt.data.schema.SchemaRules getRules() {
        return rules;
    }

    @Override
    public void openConnection(DataSchemaExecutor executor) throws SchemaMigrateException {
        rules = (SchemaRules)executor.getRules();

        try {
            connection = executor.getConfiguration().getDatabaseDriver().createConnection(
                executor.getConfiguration().getConnectionString()
            );
        } catch (DataException e) {
            throw new SchemaMigrateException("Cannot open connection", e);
        }

        try {
            characterSets = buildCharacterSets();
            collations = buildCollations(connection);
            storageEngines = buildStorageEngines();
        } catch (SQLException e) {
            throw new SchemaMigrateException("Cannot setup SQL generator", e);
        }
    }

    private Map<String, CharacterSet> buildCharacterSets() throws SQLException {
        Map<String, CharacterSet> characterSets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        try (
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW CHARACTER SET")
        ) {
            while (rs.next()) {
                CharacterSet characterSet = new CharacterSet(
                    rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4)
                );
                characterSets.put(characterSet.getName(), characterSet);
            }
        }

        return characterSets;
    }

    static Map<String, Collation> buildCollations(Connection connection) throws SQLException {
        Map<String, Collation> collations = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        try (
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW COLLATION")
        ) {
            while (rs.next()) {
                Collation collation = new Collation(
                    rs.getString(1), rs.getString(2), rs.getLong(3), rs.getString(4), rs.getString(5), rs.getLong(6)
                );
                collations.put(collation.getName(), collation);
            }
        }

        return collations;
    }

    private Map<String, StorageEngine> buildStorageEngines() throws SQLException {
        Map<String, StorageEngine> storageEngines = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        try (
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW ENGINES")
        ) {
            while (rs.next()) {
                StorageEngine storageEngine = new StorageEngine(
                    rs.getString(1
                    ), rs.getString(2), rs.getString(3)
                );
                storageEngines.put(storageEngine.getName().toLowerCase(), storageEngine);
            }
        }

        return storageEngines;
    }

    @Override
    public String getStatementSeparator() {
        return ";";
    }

    @Override
    protected String escapeComment(String comment) {
        return "-- " + comment;
    }

    @Override
    protected DataSchemaReader createDataSchemaReader() throws SchemaMigrateException {
        return new DataSchemaReader(connection);
    }

    @Override
    protected void writeUseStatement() throws SchemaMigrateException {
        String database = null;

        try (
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT DATABASE()")
        ) {
            while (rs.next()) {
                database = rs.getString(1);
            }
        } catch (SQLException e) {
            throw new SchemaMigrateException("Cannot get database name", e);
        }

        addStatement(SqlStatementType.USE_STATEMENT, "USE `%s`", database);
    }

    @Override
    public void close() throws Exception {
        if (!closed) {
            if (connection != null) {
                connection.close();
                connection = null;
            }

            closed = true;
        }
    }

    private void writeAlterTable(DataSchemaTable table) {
        pushStatement("ALTER TABLE `%s`", table.getName());
    }

    @Override
    protected void writeDropForeignKey(DataSchemaTable table, String foreign) {
        writeAlterTable(table);

        addStatement("  DROP FOREIGN KEY `%s`", foreign);
    }

    @Override
    protected void writeForeignKeyCreate(DataSchemaTable table, DataSchemaForeignKey foreign) {
        writeAlterTable(table);

        addStatement("  ADD CONSTRAINT FOREIGN KEY (`%s`) REFERENCES `%s` (`%s`)",
            foreign.getField(),
            foreign.getLinkTable(),
            foreign.getLinkField()
        );
    }

    @Override
    protected void writeTableCreate(DataSchemaTable table) throws SchemaMigrateException {
        pushStatement("CREATE TABLE `%s` (", table.getName());

        List<String> lines = new ArrayList<String>();

        for (DataSchemaField field : table.getFields().values()) {
            lines.add(writeField(field, null));
        }

        for (DataSchemaIndex index : table.getIndexes()) {
            lines.add(writeIndex(index));
        }

        for (int i = 0; i < lines.size(); i++) {
            StringBuilder sb = new StringBuilder();

            sb.append(String.format("  %s", lines.get(i)));

            if (i != lines.size() - 1) {
                sb.append(",");
            }

            pushStatement(sb.toString());
        }

        addStatement(") ENGINE=%s DEFAULT CHARSET=%s COLLATE=%s",
            rules.getDefaultEngine(), rules.getDefaultCharset(), rules.getDefaultCollation()
        );
    }

    private String writeIndex(DataSchemaIndex index) throws SchemaMigrateException {
        StringBuilder sb = new StringBuilder();

        switch (index.getType()) {
            case PRIMARY:
                sb.append(String.format("PRIMARY KEY (`%s`)", StringUtils.join(index.getFields(), "`, `")));
                break;

            case INDEX:
                sb.append(String.format("KEY (`%s`)", StringUtils.join(index.getFields(), "`, `")));
                break;

            case UNIQUE:
                sb.append(String.format("UNIQUE (`%s`)", StringUtils.join(index.getFields(), "`, `")));
                break;

            default:
                throw new SchemaMigrateException("Unknown index type");
        }

        return sb.toString();
    }

    @Override
    protected void writeTableUpdate(DataSchemaTableDifference table) {
        writeAlterTable(table.getSchema());

        List<String> parts = new ArrayList<String>();

        if (!StringUtils.equalsIgnoreCase(table.getOldSchema().getDefaultCharset(), table.getSchema().getDefaultCharset())) {
            parts.add(String.format("DEFAULT CHARSET %s", table.getSchema().getDefaultCharset()));
        }
        if (!StringUtils.equalsIgnoreCase(table.getOldSchema().getDefaultCollation(), table.getSchema().getDefaultCollation())) {
            parts.add(String.format("COLLATE %s", table.getSchema().getDefaultCollation()));
        }
        if (!StringUtils.equalsIgnoreCase(table.getOldSchema().getEngine(), table.getSchema().getEngine())) {
            parts.add(String.format("ENGINE %s", table.getSchema().getEngine()));
        }

        addStatement("  %s", StringUtils.join(parts, " "));
    }

    @Override
    protected void writeFieldDrop(DataSchemaTable table, String field) {
        writeAlterTable(table);

        addStatement("  DROP `%s`", field);
    }

    @Override
    protected void writeFieldUpdate(DataSchemaTable table, DataSchemaFieldDifference field) throws SchemaMigrateException {
        writeAlterTable(table);

        addStatement("  CHANGE `%s` %s", field.getField().getName(),
            writeField(field.getField(), field.getOldField())
        );
    }

    private String writeField(DataSchemaField field, DataSchemaField oldField) throws SchemaMigrateException {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("`%s` %s%s", field.getName(), rules.getDbType(field.getType()), dataTypeLength(field)));

        if (rules.dbTypeSupportsSign(field.getType()) && field.isUnsigned()) {
            sb.append(" UNSIGNED");
        }

        if (field.isAutoIncrement()) {
            sb.append(" AUTO_INCREMENT");
        }

        if (rules.dbTypeSupportsCharset(field.getType())) {
            if (oldField != null && !StringUtils.equals(oldField.getCharacterSet(), field.getCharacterSet())) {
                sb.append(String.format(" CHARACTER SET %s", field.getCharacterSet()));
            }

            if (oldField != null && !StringUtils.equals(oldField.getCollation(), field.getCollation())) {
                sb.append(String.format(" COLLATE %s", field.getCollation()));
            }
        }

        if (!field.isNullable()) {
            sb.append(" NOT NULL");
        }

        return sb.toString();
    }

    private String dataTypeLength(DataSchemaField field) throws SchemaMigrateException {
        int length = field.getLength();

        int fixedLength = rules.getFixedDbTypeLength(field.getType());

        if (fixedLength != -1) {
            length = fixedLength;
        }

        if (length == -1) {
            if (dbTypeRequiresLength(field.getType())) {
                throw new SchemaMigrateException(String.format("Data type '%s' requires a length", field.getType()));
            } else {
                return "";
            }
        }

        if (!rules.dbTypeSupportsLength(field.getType())) {
            return "";
        }

        if (field.getPositions() == -1) {
            return String.format("(%d)", length);
        }

        return String.format("(%d,%d)", length, field.getPositions());
    }

    @Override
    protected void writeFieldCreate(DataSchemaTable table, DataSchemaField field) throws SchemaMigrateException {
        writeAlterTable(table);

        addStatement("  ADD %s", writeField(field, null));
    }

    @Override
    protected void writeIndexDrop(DataSchemaTable table, String index) {
        writeAlterTable(table);

        addStatement("  DROP INDEX `%s`", index);
    }

    @Override
    protected void writeIndexCreate(DataSchemaTable table, DataSchemaIndex index) throws SchemaMigrateException {
        writeAlterTable(table);

        String indexType;

        switch (index.getType()) {
            case INDEX:
                indexType = "INDEX";
                break;

            case UNIQUE:
                indexType = "UNIQUE";
                break;

            default:
                throw new SchemaMigrateException("Unexpected index type");
        }

        addStatement("  ADD %s (`%s`)", indexType, StringUtils.join(index.getFields(), "`, `"));
    }

    @Override
    protected void writeTableDrop(String table) {
        addStatement("DROP TABLE `%s`", table);
        addNewline();
    }
}
