package nl.gmt.data.migrate.mysql;

import org.apache.commons.lang.StringUtils;

class Collation {
    private final String name;
    private final String characterSet;
    private final long id;
    private final boolean default_;
    private final boolean compiled;
    private final long sortLength;

    public Collation(String name, String characterSet, long id, String default_, String compiled, long sortLength) {
        this.name = name;
        this.characterSet = characterSet;
        this.id = id;
        this.default_ = StringUtils.equalsIgnoreCase(default_, "yes");
        this.compiled = StringUtils.equalsIgnoreCase(compiled, "yes");
        this.sortLength = sortLength;
    }

    public String getName() {
        return name;
    }

    public String getCharacterSet() {
        return characterSet;
    }

    public long getId() {
        return id;
    }

    public boolean isDefault() {
        return default_;
    }

    public boolean isCompiled() {
        return compiled;
    }

    public long getSortLength() {
        return sortLength;
    }
}
