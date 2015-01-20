package nl.gmt.data.schema;

public class SchemaClass extends SchemaClassBase {
    private String dbName;
    private String persister;
    private SchemaClassIdProperty idProperty;
    private SchemaClassIdProperty resolvedIdProperty;
    private String resolvedDbName;

    SchemaClass(SchemaParserLocation location) {
        super(location);
    }

    public String getDbName() {
        return dbName;
    }

    void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getPersister() {
        return persister;
    }

    void setPersister(String persister) {
        this.persister = persister;
    }

    public SchemaClassIdProperty getIdProperty() {
        return idProperty;
    }

    void setIdProperty(SchemaClassIdProperty idProperty) {
        this.idProperty = idProperty;
    }

    public SchemaClassIdProperty getResolvedIdProperty() {
        return resolvedIdProperty;
    }

    void setResolvedIdProperty(SchemaClassIdProperty resolvedIdProperty) {
        this.resolvedIdProperty = resolvedIdProperty;
    }

    public String getResolvedDbName() {
        return resolvedDbName;
    }

    void setResolvedDbName(String resolvedDbName) {
        this.resolvedDbName = resolvedDbName;
    }
}
