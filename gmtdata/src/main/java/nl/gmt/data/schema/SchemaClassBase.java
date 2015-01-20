package nl.gmt.data.schema;

import java.util.*;

public abstract class SchemaClassBase extends SchemaAnnotatableElement implements Comparable<SchemaClassBase> {
    private final Map<String, SchemaProperty> properties = new HashMap<>();
    private final Map<String, SchemaProperty> unmodifiableProperties = Collections.unmodifiableMap(properties);
    private final Map<String, SchemaForeignBase> foreigns = new HashMap<>();
    private final Map<String, SchemaForeignBase> unmodifiableForeigns = Collections.unmodifiableMap(foreigns);
    private final List<SchemaIndex> indexes = new ArrayList<>();
    private final List<SchemaIndex> unmodifiableIndexes = Collections.unmodifiableList(indexes);
    private final List<SchemaField> fields = new ArrayList<>();
    private final List<SchemaField> unmodifiableFields = Collections.unmodifiableList(fields);
    private final List<String> mixins = new ArrayList<>();
    private final List<String> unmodifiableMixins = Collections.unmodifiableList(mixins);
    private String name;
    private String packageName;

    SchemaClassBase(SchemaParserLocation location) {
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

    public Map<String, SchemaProperty> getProperties() {
        return unmodifiableProperties;
    }

    public Map<String, SchemaForeignBase> getForeigns() {
        return unmodifiableForeigns;
    }

    public List<SchemaIndex> getIndexes() {
        return unmodifiableIndexes;
    }

    public List<SchemaField> getFields() {
        return unmodifiableFields;
    }

    public List<String> getMixins() {
        return unmodifiableMixins;
    }

    void addProperty(SchemaProperty property) {
        properties.put(property.getName(), property);
        fields.add(property);
    }

    void addForeign(SchemaForeignBase foreign) {
        foreigns.put(foreign.getName(), foreign);
        fields.add(foreign);
    }

    void addIndex(SchemaIndex index) {
        indexes.add(index);
    }

    void addMixin(String mixin) {
        mixins.add(mixin);
    }

    @Override
    public String toString() {
        return getFullName();
    }

    @Override
    public int compareTo(SchemaClassBase other) {
        return name.compareTo(other.name);
    }
}
