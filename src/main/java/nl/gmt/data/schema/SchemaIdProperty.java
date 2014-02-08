package nl.gmt.data.schema;

public class SchemaIdProperty extends SchemaIdPropertyBase {
    private String name;
    private String foreignPostfix;

    SchemaIdProperty(SchemaParserLocation location) {
        super(location);
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getForeignPostfix() {
        return foreignPostfix;
    }

    void setForeignPostfix(String foreignPostfix) {
        this.foreignPostfix = foreignPostfix;
    }

    @Override
    public String toString() {
        return name;
    }
}
