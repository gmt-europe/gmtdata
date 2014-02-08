package nl.gmt.data.schema;

public abstract class SchemaForeignBase extends SchemaAnnotatableElement implements SchemaField {
    private SchemaForeignType type;
    private String name;
    private String className;
    private SchemaCascadeType cascade = SchemaCascadeType.NONE;

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

    public SchemaCascadeType getCascade() {
        return cascade;
    }

    void setCascade(SchemaCascadeType cascade) {
        this.cascade = cascade;
    }

    @Override
    public String toString() {
        return name;
    }
}
