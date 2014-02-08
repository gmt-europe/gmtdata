package nl.gmt.data.schema;

import org.apache.commons.lang.StringUtils;

public final class SchemaParserLocation {
    private String fileName;
    private int line;
    private int column;

    public SchemaParserLocation(String fileName, int line, int column) {
        this.fileName = fileName;
        this.line = line;
        this.column = column;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return String.format("%s(%d,%d)", fileName, line, column);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SchemaParserLocation)) return false;

        SchemaParserLocation that = (SchemaParserLocation) o;

        if (column != that.column) return false;
        if (line != that.line) return false;
        if (!StringUtils.equals(fileName,  that.fileName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = fileName != null ? fileName.hashCode() : 0;
        result = 31 * result + line;
        result = 31 * result + column;
        return result;
    }
}
