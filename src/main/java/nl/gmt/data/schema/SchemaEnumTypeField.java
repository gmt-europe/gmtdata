package nl.gmt.data.schema;

public class SchemaEnumTypeField extends SchemaAnnotatableElement {
    private String name;
    private int value;

    SchemaEnumTypeField(SchemaParserLocation location) {
        super(location);
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    void setValue(int value) {
        this.value = value;
    }
}