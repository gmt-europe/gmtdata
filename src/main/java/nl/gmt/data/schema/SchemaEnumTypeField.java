package nl.gmt.data.schema;

public class SchemaEnumTypeField extends SchemaAnnotatableElement {
    private String name;
    private String value;

    SchemaEnumTypeField(SchemaParserLocation location) {
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
