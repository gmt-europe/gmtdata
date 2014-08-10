package nl.gmt.data.migrate;

import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaClass;
import nl.gmt.data.schema.SchemaRules;

import java.util.Map;
import java.util.TreeMap;

public class DataSchema {
    private final Map<String, DataSchemaTable> tables;

    private DataSchema(Map<String, DataSchemaTable> tables) {
        this.tables = tables;
    }

    public static DataSchema fromSchema(Schema schema, SchemaRules rules) throws SchemaMigrateException {
        DataSchemaFactory factory = rules.newDataSchemaFactory();

        Map<String, DataSchemaTable> tables = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (SchemaClass klass : schema.getClasses().values()) {
            DataSchemaTable table = factory.createClass(klass, schema, rules);

            tables.put(table.getName(), table);
        }

        return new DataSchema(tables);
    }

    public static DataSchema fromReader(DataSchemaReader reader) throws SchemaMigrateException {
        return new DataSchema(reader.getTables());
    }

    public static Map<String, ChangedState> getChanges(Iterable<String> currentState, Iterable<String> newState) {
        Map<String, ChangedState> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (String item : currentState) {
            result.put(item, ChangedState.REMOVED);
        }

        for (String item : newState) {
            if (result.containsKey(item)) {
                result.put(item, ChangedState.EXISTING);
            } else {
                result.put(item, ChangedState.NEW);
            }
        }

        return result;
    }

    public Map<String, DataSchemaTable> getTables() {
        return tables;
    }
}
