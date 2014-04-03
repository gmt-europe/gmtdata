package nl.gmt.data.migrate;

import nl.gmt.data.schema.*;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DataSchemaIndex {
    private SchemaIndexType type;
    private String name;
    private final List<String> fields;

    public DataSchemaIndex() {
        fields = new ArrayList<>();
    }

    public SchemaIndexType getType() {
        return type;
    }

    public void setType(SchemaIndexType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getFields() {
        return fields;
    }

    public boolean equals(DataSchemaIndex other) {
        if (this == other)
            return true;

        if (
            type != other.type ||
            fields.size() != other.fields.size()
        )
            return false;

        for (int i = 0; i < fields.size(); i++) {
            if (!StringUtils.equalsIgnoreCase(fields.get(i), other.fields.get(i)))
                return false;
        }

        return true;
    }

    public void createName(DataSchemaTable table,  DataSchemaTable currentTable) {
        int nameOffset = 1;
        String template = "IX_" + table.getName() + "_%d";

        String indexName;

        while (true) {
            indexName = String.format(template, nameOffset);

            if (
                nameExists(indexName, table.getIndexes()) ||
                (currentTable != null && nameExists(indexName, currentTable.getIndexes()))
            )
                nameOffset++;
            else
                break;
        }

        name = indexName;
    }

    private boolean nameExists(String indexName, List<DataSchemaIndex> indexes) {
        for (DataSchemaIndex index : indexes) {
            if (
                index.name != null &&
                StringUtils.equalsIgnoreCase(index.name, indexName)
            )
                return true;
        }

        return false;
    }
}