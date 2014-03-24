package nl.gmt.data.migrate.mysql;

class StorageEngine {
    private final String name;
    private final String comment;
    private final StorageEngineSupport support;
    private final boolean supported;

    StorageEngine(String name, String comment, String support) {
        this.name = name;
        this.comment = comment;

        switch (support.toLowerCase()) {
            case "yes": this.support = StorageEngineSupport.YES; break;
            case "no": this.support = StorageEngineSupport.NO; break;
            case "default": this.support = StorageEngineSupport.DEFAULT; break;
            case "disabled": this.support = StorageEngineSupport.DISABLED; break;
            default: this.support = StorageEngineSupport.UNKNOWN; break;
        }

        supported = this.support == StorageEngineSupport.YES || this.support == StorageEngineSupport.DEFAULT;
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public StorageEngineSupport getSupport() {
        return support;
    }

    public boolean isSupported() {
        return supported;
    }
}
