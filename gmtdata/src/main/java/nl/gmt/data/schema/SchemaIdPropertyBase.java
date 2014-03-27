package nl.gmt.data.schema;

public abstract class SchemaIdPropertyBase extends SchemaAnnotatableElement {
    private String type;
    private SchemaIdAutoIncrement autoIncrement = SchemaIdAutoIncrement.UNSET;
    private SchemaResolvedDataType resolvedDataType;
    private SchemaGenerator generator;

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

    public SchemaResolvedDataType getResolvedDataType() {
        return resolvedDataType;
    }

    void setResolvedDataType(SchemaResolvedDataType resolvedDataType) {
        this.resolvedDataType = resolvedDataType;
    }

    public SchemaGenerator getGenerator() {
        return generator;
    }

    void setGenerator(SchemaGenerator generator) {
        this.generator = generator;
    }
}
