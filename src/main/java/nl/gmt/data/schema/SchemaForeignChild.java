package nl.gmt.data.schema;

public class SchemaForeignChild extends SchemaForeignBase {
    private String classProperty;

    SchemaForeignChild(SchemaParserLocation location) {
        super(SchemaForeignType.CHILD, location);
    }

    public String getClassProperty() {
        return classProperty;
    }

    void setClassProperty(String classProperty) {
        this.classProperty = classProperty;
    }
}
