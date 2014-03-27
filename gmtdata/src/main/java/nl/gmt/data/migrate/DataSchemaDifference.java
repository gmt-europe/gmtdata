package nl.gmt.data.migrate;

import java.util.*;

public class DataSchemaDifference {
    private final List<String> removedTables = new ArrayList<>();
    private final Map<String, DataSchemaTable> newTables = new HashMap<>();
    private final Map<String, DataSchemaTableDifference> changedTables = new HashMap<>();

    public DataSchemaDifference(DataSchema currentSchema, DataSchema newSchema, DataSchemaExecutor executor) throws SchemaMigrateException {
        Map<String, ChangedState> tables = DataSchema.getChanges(currentSchema.getTables().keySet(), newSchema.getTables().keySet());

        for (Map.Entry<String, ChangedState> table : tables.entrySet()) {
            switch (table.getValue()) {
                case NEW:
                    DataSchemaTable newTable = newSchema.getTables().get(table.getKey());

                    if (executor.getConfiguration().isNoConstraintsOrIndexes())
                        newTable.stripForeignKeysAndIndexes();

                    newTables.put(newTable.getName(), newTable);
                    break;

                case REMOVED:
                    removedTables.add(table.getKey());
                    break;

                case EXISTING:
                    DataSchemaTableDifference difference = new DataSchemaTableDifference(
                        currentSchema.getTables().get(table.getKey()),
                        newSchema.getTables().get(table.getKey()),
                        executor
                    );

                    if (difference.isDiffer())
                        changedTables.put(table.getKey(), difference);
                    break;
            }
        }
    }

    public List<String> getRemovedTables() {
        return removedTables;
    }

    public Map<String, DataSchemaTable> getNewTables() {
        return newTables;
    }

    public Map<String, DataSchemaTableDifference> getChangedTables() {
        return changedTables;
    }
}
