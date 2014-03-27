package nl.gmt.data.schema;

public abstract class SchemaElement {
    private final SchemaParserLocation location;

    SchemaElement(SchemaParserLocation location) {
        this.location = location;
    }

    public SchemaParserLocation getLocation() {
        return location;
    }
}
