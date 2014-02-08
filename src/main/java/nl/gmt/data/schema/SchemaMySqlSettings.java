package nl.gmt.data.schema;

public class SchemaMySqlSettings extends SchemaAnnotatableElement {
    private String engine;
    private String charset;
    private String collation;

    SchemaMySqlSettings(SchemaParserLocation location) {
        super(location);
    }

    public String getEngine() {
        return engine;
    }

    void setEngine(String engine) {
        this.engine = engine;
    }

    public String getCharset() {
        return charset;
    }

    void setCharset(String charset) {
        this.charset = charset;
    }

    public String getCollation() {
        return collation;
    }

    void setCollation(String collation) {
        this.collation = collation;
    }
}
