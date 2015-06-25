package nl.gmt.data.contrib.postgres;

public class JSONBUserType extends PGobjectUserType {
    public JSONBUserType() {
        super("jsonb");
    }
}
