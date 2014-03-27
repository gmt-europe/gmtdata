package nl.gmt.data.schema;

public abstract class SchemaForeignBase extends SchemaAnnotatableElement implements SchemaField, Comparable<SchemaField> {
    private SchemaForeignType type;
    private String name;
    private String className;

    SchemaForeignBase(SchemaForeignType type, SchemaParserLocation location) {
        super(location);

        this.type = type;
    }

    public SchemaForeignType getType() {
        return type;
    }

    void setType(SchemaForeignType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(SchemaField other) {
        return name.compareTo(other.getName());
    }
}
