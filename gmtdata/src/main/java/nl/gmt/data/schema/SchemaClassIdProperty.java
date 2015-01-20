package nl.gmt.data.schema;

public class SchemaClassIdProperty extends SchemaIdPropertyBase implements SchemaPropertyField {
    private String dbName;
    private String dbSequence;
    private SchemaCompositeId compositeId;
    private String resolvedDbName;

    public SchemaClassIdProperty(SchemaParserLocation location) {
        super(location);
    }

    @Override
    public String getDbName() {
        return dbName;
    }

    void setDbName(String dbName) {
        this.dbName = dbName;
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

    @Override
    public String getResolvedDbName() {
        return resolvedDbName;
    }

    void setResolvedDbName(String resolvedDbIdName) {
        this.resolvedDbName = resolvedDbIdName;
    }

    @Override
    public String getName() {
        return "Id";
    }

    @Override
    public int compareTo(SchemaField other) {
        return getName().compareTo(other.getName());
    }
}
