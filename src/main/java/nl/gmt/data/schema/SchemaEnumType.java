package nl.gmt.data.schema;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SchemaEnumType extends SchemaAnnotatableElement {
    private String name;
    private final Map<String, SchemaEnumTypeField> fields = new HashMap<>();
    private final Map<String, SchemaEnumTypeField> unmodifiableFields = Collections.unmodifiableMap(fields);

    public SchemaEnumType(SchemaParserLocation location) {
        super(location);
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public Map<String, SchemaEnumTypeField> getFields() {
        return unmodifiableFields;
    }

    void addField(SchemaEnumTypeField field) {
        fields.put(field.getName(),  field);
    }
}
