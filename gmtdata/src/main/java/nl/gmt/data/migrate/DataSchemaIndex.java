package nl.gmt.data.migrate;

import nl.gmt.data.schema.SchemaIndexType;
import nl.gmt.data.schema.SchemaRules;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DataSchemaIndex {
    private SchemaIndexType type;
    private String name;
    private final List<String> fields;
    private String strategy;
    private String filter;

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

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public boolean equals(DataSchemaIndex other, SchemaRules rules) throws SchemaMigrateException {
        if (this == other)
            return true;

        if (
            type != other.type ||
            fields.size() != other.fields.size() ||
            !StringUtils.equalsIgnoreCase(filter, other.filter)
        )
            return false;

        for (int i = 0; i < fields.size(); i++) {
            if (!StringUtils.equalsIgnoreCase(fields.get(i), other.fields.get(i)))
                return false;
        }

        if (!(strategy == null && other.strategy == null)) {
            if (!rules.getIndexStrategy(strategy).equals(rules.getIndexStrategy(other.strategy)))
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
