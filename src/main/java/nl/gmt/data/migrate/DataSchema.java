package nl.gmt.data.migrate;

import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaClass;

import java.util.HashMap;
import java.util.Map;

public class DataSchema {
    private Map<String, DataSchemaTable> tables;

    private DataSchema(Map<String, DataSchemaTable> tables) {
        this.tables = tables;
    }

    public static DataSchema fromSchema(Schema schema, SqlGenerator generator) throws SchemaMigrateException {
        Map<String, DataSchemaTable> tables = new HashMap<>();

        for (SchemaClass klass : schema.getClasses().values()) {
            DataSchemaTable table = DataSchemaTable.createFromSchemaClass(klass, schema, generator);

            tables.put(table.getName(), table);
        }

        return new DataSchema(tables);
    }

    public static DataSchema fromReader(DataSchemaReader reader) throws SchemaMigrateException {
        return new DataSchema(reader.getTables());
    }

    public static Map<String, ChangedState> getChanges(Iterable<String> currentState, Iterable<String> newState) {
        Map<String, ChangedState> result = new HashMap<>();

        for (String item : currentState) {
            result.put(item, ChangedState.REMOVED);
        }

        for (String item : newState) {
            if (result.containsKey(item))
                result.put(item, ChangedState.EXISTING);
            else
                result.put(item, ChangedState.NEW);
        }

        return result;
    }

    public Map<String, DataSchemaTable> getTables() {
        return tables;
    }
}
