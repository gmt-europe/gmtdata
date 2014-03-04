package nl.gmt.data.schema;

public class SchemaProperty extends SchemaDataTypeBase implements SchemaField {
    private SchemaResolvedDataType resolvedDataType;
    private String resolvedDbName;

    SchemaProperty(SchemaParserLocation location) {
        super(location);
    }

    public SchemaResolvedDataType getResolvedDataType() {
        return resolvedDataType;
    }

    void setResolvedDataType(SchemaResolvedDataType resolvedDataType) {
        this.resolvedDataType = resolvedDataType;
    }

    public String getResolvedDbName() {
        return resolvedDbName;
    }

    void setResolvedDbName(String resolvedDbName) {
        this.resolvedDbName = resolvedDbName;
    }

    @Override
    public int compareTo(SchemaField other) {
        return getName().compareTo(other.getName());
    }
}
