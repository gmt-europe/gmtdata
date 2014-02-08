package nl.gmt.data.schema;

public class SchemaException extends Exception {
    private SchemaParserLocation location;

    public SchemaException() {
    }

    public SchemaException(String message) {
        super(message);
    }

    public SchemaException(String message, Throwable cause) {
        super(message, cause);
    }

    public SchemaException(String message, SchemaParserLocation location) {
        super(message);

        this.location = location;
    }

    public SchemaException(String message, SchemaParserLocation location, Throwable cause) {
        super(message, cause);

        this.location = location;
    }

    public SchemaException(Throwable cause) {
        super(cause);
    }

    public SchemaException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SchemaParserLocation getLocation() {
        return location;
    }
}
