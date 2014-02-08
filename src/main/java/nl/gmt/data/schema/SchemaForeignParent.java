package nl.gmt.data.schema;

public class SchemaForeignParent extends SchemaForeignBase {
    private String dbName;
    private SchemaIndexType indexType = SchemaIndexType.UNSET;
    private SchemaAllowNull allowNull = SchemaAllowNull.UNSET;
    private String resolvedDbName;

    SchemaForeignParent(SchemaParserLocation location) {
        super(SchemaForeignType.PARENT, location);
    }

    public String getDbName() {
        return dbName;
    }

    void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public SchemaIndexType getIndexType() {
        return indexType;
    }

    void setIndexType(SchemaIndexType indexType) {
        this.indexType = indexType;
    }

    public SchemaAllowNull getAllowNull() {
        return allowNull;
    }

    void setAllowNull(SchemaAllowNull allowNull) {
        this.allowNull = allowNull;
    }

    public String getResolvedDbName() {
        return resolvedDbName;
    }

    void setResolvedDbName(String resolvedDbName) {
        this.resolvedDbName = resolvedDbName;
    }
}
