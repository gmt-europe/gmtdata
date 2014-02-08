package nl.gmt.data.migrate;

public class Manifest {
    private String version;
    private String schemaHash;

    public Manifest(String version, String schemaHash) {
        this.version = version;
        this.schemaHash = schemaHash;
    }

    public String getVersion() {
        return version;
    }

    public String getSchemaHash() {
        return schemaHash;
    }
}
