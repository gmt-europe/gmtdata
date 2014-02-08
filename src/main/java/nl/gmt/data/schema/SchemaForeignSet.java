package nl.gmt.data.schema;

public class SchemaForeignSet extends SchemaForeignBase {
    private String linksHere;
    private String linksThere;
    private boolean inverse;

    SchemaForeignSet(SchemaParserLocation location, boolean inverse) {
        super(SchemaForeignType.SET, location);

        this.inverse = inverse;
    }

    public String getLinksHere() {
        return linksHere;
    }

    void setLinksHere(String linksHere) {
        this.linksHere = linksHere;
    }

    public String getLinksThere() {
        return linksThere;
    }

    void setLinksThere(String linksThere) {
        this.linksThere = linksThere;
    }

    public boolean isInverse() {
        return inverse;
    }

    void setInverse(boolean inverse) {
        this.inverse = inverse;
    }
}
