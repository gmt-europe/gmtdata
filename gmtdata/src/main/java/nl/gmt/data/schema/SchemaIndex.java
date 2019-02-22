package nl.gmt.data.schema;

import org.apache.commons.lang.StringUtils;

import java.util.List;

public class SchemaIndex extends SchemaAnnotatableElement {
    private List<String> fields;
    private List<String> includeFields;
    private SchemaIndexType type = SchemaIndexType.UNSET;
    private String strategy;
    private String filter;

    SchemaIndex(SchemaParserLocation location) {
        super(location);
    }

    public List<String> getFields() {
        return fields;
    }

    void setFields(List<String> fields) {
        this.fields = fields;
    }

    public List<String> getIncludeFields() {
        return includeFields;
    }

    public void setIncludeFields(List<String> includeFields) {
        this.includeFields = includeFields;
    }

    public SchemaIndexType getType() {
        return type;
    }

    void setType(SchemaIndexType type) {
        this.type = type;
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

    String toSmallString() {
        String result = String.format("(`%s`)", StringUtils.join(fields, "` `"));

        if (type == SchemaIndexType.UNIQUE || type == SchemaIndexType.PRIMARY)
            result = type.toString().toLowerCase() + " " + result;

        if (strategy != null)
            result += " strategy " + strategy;

        return result;
    }

    boolean conflictsWith(SchemaIndex conflicting) {
        if (filter != null || conflicting.filter != null || fields.size() > conflicting.fields.size())
            return false;

        for (int i = 0; i < fields.size(); i++) {
            if (!StringUtils.equalsIgnoreCase(fields.get(i), conflicting.fields.get(i)))
                return false;
        }

        if (fields.size() < conflicting.fields.size())
            return type != SchemaIndexType.UNIQUE;

        return true;
    }

    boolean conflictsWith(SchemaForeignParent foreign) {
        if (filter != null)
            return false;

        if (
            fields.size() == 1 &&
            StringUtils.equalsIgnoreCase(fields.get(0), foreign.getName())
        )
            return foreign.getIndexType() == SchemaIndexType.UNIQUE || type != SchemaIndexType.UNIQUE;

        return false;
    }
}
