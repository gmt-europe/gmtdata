package nl.gmt.data.migrate;

import nl.gmt.data.schema.SchemaIndexType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataSchemaTable extends DataSchemaObject {
    private String name;
    private final Map<String, DataSchemaField> fields;
    private final List<DataSchemaForeignKey> foreignKeys;
    private List<DataSchemaIndex> indexes;
    private int autoIncrement;

    public DataSchemaTable() {
        fields = new HashMap<>();
        foreignKeys = new ArrayList<>();
        indexes = new ArrayList<>();
    }

    public void addField(DataSchemaField field) {
        fields.put(field.getName(), field);
    }

    void stripForeignKeysAndIndexes() {
        foreignKeys.clear();

        List<DataSchemaIndex> oldIndexes = indexes;

        indexes = new ArrayList<>();

        for (DataSchemaIndex index : oldIndexes) {
            if (index.getType() == SchemaIndexType.PRIMARY)
                indexes.add(index);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, DataSchemaField> getFields() {
        return fields;
    }

    public List<DataSchemaForeignKey> getForeignKeys() {
        return foreignKeys;
    }

    public List<DataSchemaIndex> getIndexes() {
        return indexes;
    }

    public int getAutoIncrement() {
        return autoIncrement;
    }

    public void setAutoIncrement(int autoIncrement) {
        this.autoIncrement = autoIncrement;
    }
}
