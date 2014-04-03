package nl.gmt.data.schema;

public abstract class SchemaDataTypeBase extends SchemaAnnotatableElement {
    private String name;
    private String dbName;
    private String rawType;
    private Class<?> nativeType;
    private String userType;
    private String enumType;
    private int length = -1;
    private int positions = -1;
    private SchemaAllowNull allowNull = SchemaAllowNull.UNSET;
    private SchemaSigned signed = SchemaSigned.UNSET;
    private SchemaLazy lazy = SchemaLazy.UNSET;
    private String rawDbType;
    private SchemaDbType dbType = SchemaDbType.UNSET;
    private SchemaIndexType indexType = SchemaIndexType.UNSET;
    private String type;

    SchemaDataTypeBase(SchemaParserLocation location) {
        super(location);
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getDbName() {
        return dbName;
    }

    void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getRawType() {
        return rawType;
    }

    void setRawType(String rawType) {
        this.rawType = rawType;
    }

    public Class<?> getNativeType() {
        return nativeType;
    }

    void setNativeType(Class<?> nativeType) {
        this.nativeType = nativeType;
    }

    public String getUserType() {
        return userType;
    }

    void setUserType(String userType) {
        this.userType = userType;
    }

    public String getEnumType() {
        return enumType;
    }

    void setEnumType(String enumType) {
        this.enumType = enumType;
    }

    public int getLength() {
        return length;
    }

    void setLength(int length) {
        this.length = length;
    }

    public int getPositions() {
        return positions;
    }

    void setPositions(int positions) {
        this.positions = positions;
    }

    public SchemaAllowNull getAllowNull() {
        return allowNull;
    }

    void setAllowNull(SchemaAllowNull allowNull) {
        this.allowNull = allowNull;
    }

    public SchemaSigned getSigned() {
        return signed;
    }

    void setSigned(SchemaSigned signed) {
        this.signed = signed;
    }

    public SchemaLazy getLazy() {
        return lazy;
    }

    void setLazy(SchemaLazy lazy) {
        this.lazy = lazy;
    }

    public String getRawDbType() {
        return rawDbType;
    }

    void setRawDbType(String rawDbType) {
        this.rawDbType = rawDbType;
    }

    public SchemaDbType getDbType() {
        return dbType;
    }

    void setDbType(SchemaDbType dbType) {
        this.dbType = dbType;
    }

    public SchemaIndexType getIndexType() {
        return indexType;
    }

    void setIndexType(SchemaIndexType indexType) {
        this.indexType = indexType;
    }

    public String getType() {
        return type;
    }

    void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return name;
    }
}
