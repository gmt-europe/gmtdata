package nl.gmt.data.contrib.postgres;

public class JSONUserType extends PGobjectUserType {
    public JSONUserType() {
        super("json");
    }
}
