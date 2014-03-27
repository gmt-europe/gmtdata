package nl.gmt.data.schema;

public class SchemaClassIdProperty extends SchemaIdPropertyBase {
    private String dbIdName;
    private String dbSequence;
    private SchemaCompositeId compositeId;
    private String resolvedDbIdName;

    public SchemaClassIdProperty(SchemaParserLocation location) {
        super(location);
    }

    public String getDbIdName() {
        return dbIdName;
    }

    void setDbIdName(String dbIdName) {
        this.dbIdName = dbIdName;
    }

    public String getDbSequence() {
        return dbSequence;
    }

    void setDbSequence(String dbSequence) {
        this.dbSequence = dbSequence;
    }

    public SchemaCompositeId getCompositeId() {
        return compositeId;
    }

    void setCompositeId(SchemaCompositeId compositeId) {
        this.compositeId = compositeId;
    }

    public String getResolvedDbIdName() {
        return resolvedDbIdName;
    }

    void setResolvedDbIdName(String resolvedDbIdName) {
        this.resolvedDbIdName = resolvedDbIdName;
    }
}
