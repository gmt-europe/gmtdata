package nl.gmt.data.schema;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SchemaEnumType extends SchemaAnnotatableElement implements Comparable<SchemaEnumType> {
    private String name;
    private String packageName;
    private final Map<String, SchemaEnumTypeField> fields = new HashMap<>();
    private final Map<String, SchemaEnumTypeField> unmodifiableFields = Collections.unmodifiableMap(fields);

    public SchemaEnumType(SchemaParserLocation location) {
        super(location);
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getFullName() {
        if (packageName != null) {
            return packageName + "." + name;
        }

        return name;
    }

    void setFullName(String fullName) {
        int pos = fullName.lastIndexOf('.');
        if (pos == -1) {
            name = fullName;
        } else {
            packageName = fullName.substring(0, pos);
            name = fullName.substring(pos + 1);
        }
    }

    public Map<String, SchemaEnumTypeField> getFields() {
        return unmodifiableFields;
    }

    void addField(SchemaEnumTypeField field) {
        fields.put(field.getName(),  field);
    }

    @Override
    public int compareTo(SchemaEnumType other) {
        return name.compareTo(other.name);
    }
}
