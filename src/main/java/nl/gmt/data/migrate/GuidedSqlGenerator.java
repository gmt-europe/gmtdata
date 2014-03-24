package nl.gmt.data.migrate;

import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaRules;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class GuidedSqlGenerator extends SqlGenerator {
    protected GuidedSqlGenerator(Schema schema) {
        super(schema);
    }

    protected abstract SchemaRules getRules();

    @Override
    protected void applyChanges() throws SchemaMigrateException {
        removeExistingTablesForeignKeys();

        WriteTableDeletes();

        writeTableCreates();

        updateExistingTables();

        writeTableCreatesForeignKeys();

        updateExistingTablesForeignKeys();
    }

    private void removeExistingTablesForeignKeys() {
        Map<DataSchemaTable, Map<String, Boolean>> removedKeys = new HashMap<>();

        for (DataSchemaTableDifference table : getDifference().getChangedTables().values()) {
            for (String foreign : table.getRemovedForeignKeys()) {
                Map<String, Boolean> tableKeys = removedKeys.get(table.getOldSchema());

                if (tableKeys == null) {
                    tableKeys = new HashMap<>();
                    removedKeys.put(table.getOldSchema(), tableKeys);
                }

                tableKeys.put(foreign, true);
            }
        }

        Set<String> deletingTables = new HashSet<>();

        for (String table : getDifference().getRemovedTables()) {
            deletingTables.add(table);
        }

        for (DataSchemaTable table : getCurrentSchema().getTables().values()) {
            for (DataSchemaForeignKey foreignKey : table.getForeignKeys()) {
                if (deletingTables.contains(foreignKey.getLinkTable())) {
                    Map<String, Boolean> tableKeys = removedKeys.get(table);

                    if (tableKeys == null) {
                        tableKeys = new HashMap<>();
                        removedKeys.put(table, tableKeys);
                    }

                    tableKeys.put(foreignKey.getName(), true);
                }
            }
        }

        for (Map.Entry<DataSchemaTable, Map<String, Boolean>> table : removedKeys.entrySet()) {
            for (String foreignKey : table.getValue().keySet()) {
                writeHeader("Removing foreign keys");

                writeDropForeignKey(table.getKey(), foreignKey);

                addNewline();
            }
        }
    }

    protected abstract void writeDropForeignKey(DataSchemaTable table, String foreign);

    private void WriteTableDeletes() {
        for (String table : getDifference().getRemovedTables()) {
            writeHeader("Removed tables");

            writeTableDrop(table);
        }
    }

    protected abstract void writeTableDrop(String table);

    private void writeTableCreates() throws SchemaMigrateException {
        for (DataSchemaTable table : getDifference().getNewTables().values()) {
            writeHeader("New tables");

            for (DataSchemaIndex index : table.getIndexes()) {
                index.createName(table, null);
            }

            writeTableCreate(table);
            addNewline();
        }
    }

    protected abstract void writeTableCreate(DataSchemaTable table) throws SchemaMigrateException;

    private void updateExistingTables() throws SchemaMigrateException {
        for (DataSchemaTableDifference table : getDifference().getChangedTables().values()) {
            updateExistingTable(table);
        }
    }

    private void updateExistingTable(DataSchemaTableDifference table) throws SchemaMigrateException {
        if (
            (getRules().supportsCharset() || getRules().supportsEngine()) &&
            (
                !StringUtils.equalsIgnoreCase(table.getOldSchema().getDefaultCharset(), table.getSchema().getDefaultCharset()) ||
                !StringUtils.equalsIgnoreCase(table.getOldSchema().getDefaultCollation(), table.getSchema().getDefaultCollation()) ||
                !StringUtils.equalsIgnoreCase(table.getOldSchema().getEngine(), table.getSchema().getEngine())
            )
        ) {
            writeHeader("Updating existing tables");

            writeTableUpdate(table);
            addNewline();
        }

        for (String index : table.getRemovedIndexes()) {
            writeHeader("Updating existing tables");

            writeIndexDrop(table.getSchema(), index);
            addNewline();
        }

        for (String field : table.getRemovedFields()) {
            writeHeader("Updating existing tables");

            writeFieldDrop(table.getSchema(), field);
            addNewline();
        }

        for (DataSchemaFieldDifference field : table.getChangedFields()) {
            writeHeader("Updating existing tables");

            writeFieldUpdate(table.getSchema(), field);
            addNewline();
        }

        for (DataSchemaField field : table.getNewFields().values()) {
            writeHeader("Updating existing tables");

            writeFieldCreate(table.getSchema(), field);
            addNewline();
        }

        for (DataSchemaIndex index : table.getNewIndexes()) {
            writeHeader("Updating existing tables");

            index.createName(table.getSchema(), table.getOldSchema());

            writeIndexCreate(table.getSchema(), index);
            addNewline();
        }
    }

    protected abstract void writeIndexCreate(DataSchemaTable table, DataSchemaIndex index) throws SchemaMigrateException;
    protected abstract void writeIndexDrop(DataSchemaTable table, String index);
    protected abstract void writeFieldCreate(DataSchemaTable table, DataSchemaField field) throws SchemaMigrateException;
    protected abstract void writeFieldUpdate(DataSchemaTable table, DataSchemaFieldDifference field) throws SchemaMigrateException;
    protected abstract void writeTableUpdate(DataSchemaTableDifference table);
    protected abstract void writeFieldDrop(DataSchemaTable table, String field);

    private void writeTableCreatesForeignKeys() {
        for (DataSchemaTable table : getDifference().getNewTables().values()) {
            for (DataSchemaForeignKey foreign : table.getForeignKeys()) {
                writeHeader("Foreign keys for new tables");

                foreign.createName(table, null);

                writeForeignKeyCreate(table, foreign);
                addNewline();
            }
        }
    }

    protected abstract void writeForeignKeyCreate(DataSchemaTable table, DataSchemaForeignKey foreign);

    private void updateExistingTablesForeignKeys() {
        for (DataSchemaTableDifference table : getDifference().getChangedTables().values()) {
            for (DataSchemaForeignKey foreignKey : table.getNewForeignKeys()) {
                writeHeader("New foreign key for changed tables");

                foreignKey.createName(table.getSchema(), table.getOldSchema());

                writeForeignKeyCreate(table.getSchema(), foreignKey);
                addNewline();
            }
        }
    }
}
