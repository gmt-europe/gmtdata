package nl.gmt.data.schema;

public class SchemaForeignChild extends SchemaForeignBase {
    private String classProperty;
    private String indexProperty;
    private SchemaForeignChildMode mode = SchemaForeignChildMode.SET;

    SchemaForeignChild(SchemaParserLocation location) {
        super(SchemaForeignType.CHILD, location);
    }

    public String getClassProperty() {
        return classProperty;
    }

    void setClassProperty(String classProperty) {
        this.classProperty = classProperty;
    }

    public String getIndexProperty() {
        return indexProperty;
    }

    void setIndexProperty(String indexProperty) {
        this.indexProperty = indexProperty;
    }

    public SchemaForeignChildMode getMode() {
        return mode;
    }

    void setMode(SchemaForeignChildMode mode) {
        this.mode = mode;
    }
}
