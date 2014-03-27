package nl.gmt.data.schema;

public class SchemaCompositeIdProperty extends SchemaElement {
    private String name;

    SchemaCompositeIdProperty(SchemaParserLocation location) {
        super(location);
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }
}
