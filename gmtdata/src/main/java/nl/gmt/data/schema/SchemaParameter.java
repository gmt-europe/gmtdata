package nl.gmt.data.schema;

public class SchemaParameter extends SchemaAnnotatableElement {
    private String name;
    private String value;

    SchemaParameter(SchemaParserLocation location) {
        super(location);
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

     void setValue(String value) {
        this.value = value;
    }
}
