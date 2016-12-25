package nl.gmt.data.schema;

public class SchemaClass extends SchemaClassBase {
    private String dbName;
    private String persister;
    private SchemaClassIdProperty idProperty;
    private SchemaClassIdProperty resolvedIdProperty;
    private String resolvedDbName;
    private boolean dynamicInsert;
    private boolean dynamicUpdate;

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

    public boolean isDynamicInsert() {
        return dynamicInsert;
    }

    public void setDynamicInsert(boolean dynamicInsert) {
        this.dynamicInsert = dynamicInsert;
    }

    public boolean isDynamicUpdate() {
        return dynamicUpdate;
    }

    public void setDynamicUpdate(boolean dynamicUpdate) {
        this.dynamicUpdate = dynamicUpdate;
    }
}
