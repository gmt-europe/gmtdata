package nl.gmt.data.migrate.mysql;

class CharacterSet {
    private final String name;
    private final String description;
    private final String defaultCollation;
    private final long maxLength;

    public CharacterSet(String name, String description, String defaultCollation, long maxLength) {
        this.name = name;
        this.description = description;
        this.defaultCollation = defaultCollation;
        this.maxLength = maxLength;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultCollation() {
        return defaultCollation;
    }

    public long getMaxLength() {
        return maxLength;
    }
}
