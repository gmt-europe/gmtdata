package nl.gmt.data.schema;

public abstract class SchemaIdPropertyBase extends SchemaAnnotatableElement {
    private String type;
    private SchemaIdAutoIncrement autoIncrement = SchemaIdAutoIncrement.UNSET;
    private String generator;
    private SchemaResolvedDataType resolvedDataType;

    SchemaIdPropertyBase(SchemaParserLocation location) {
        super(location);
    }

    public String getType() {
        return type;
    }

    void setType(String type) {
        this.type = type;
    }

    public SchemaIdAutoIncrement getAutoIncrement() {
        return autoIncrement;
    }

    void setAutoIncrement(SchemaIdAutoIncrement autoIncrement) {
        this.autoIncrement = autoIncrement;
    }

    public String getGenerator() {
        return generator;
    }

    void setGenerator(String generator) {
        this.generator = generator;
    }

    public SchemaResolvedDataType getResolvedDataType() {
        return resolvedDataType;
    }

    void setResolvedDataType(SchemaResolvedDataType resolvedDataType) {
        this.resolvedDataType = resolvedDataType;
    }
}
