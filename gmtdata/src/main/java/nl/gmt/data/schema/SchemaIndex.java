package nl.gmt.data.schema;

import org.apache.commons.lang.StringUtils;

import java.util.List;

public class SchemaIndex extends SchemaAnnotatableElement {
    private List<String> fields;
    private SchemaIndexType type = SchemaIndexType.UNSET;

    SchemaIndex(SchemaParserLocation location) {
        super(location);
    }

    public List<String> getFields() {
        return fields;
    }

    void setFields(List<String> fields) {
        this.fields = fields;
    }

    public SchemaIndexType getType() {
        return type;
    }

    void setType(SchemaIndexType type) {
        this.type = type;
    }

    String toSmallString() {
        String result = String.format("(`{0}`)", StringUtils.join(fields, "` `"));

        if (type == SchemaIndexType.UNIQUE || type == SchemaIndexType.PRIMARY)
            result = type.toString().toLowerCase() + " " + result;

        return result;
    }

    boolean conflictsWith(SchemaIndex conflicting) {
        if (fields.size() > conflicting.fields.size())
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
        if (
            fields.size() == 1 &&
            StringUtils.equalsIgnoreCase(fields.get(0), foreign.getName())
        )
            return foreign.getIndexType() == SchemaIndexType.UNIQUE || type != SchemaIndexType.UNIQUE;

        return false;
    }

    boolean conflictsWith(SchemaIdProperty idProperty) {
        return
            fields.size() == 1 &&
            StringUtils.equalsIgnoreCase(fields.get(0), idProperty.getName());
    }
}