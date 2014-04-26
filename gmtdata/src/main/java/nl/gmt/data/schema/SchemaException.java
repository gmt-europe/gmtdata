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
        super(addLocation(message, location));

        this.location = location;
    }

    public SchemaException(String message, SchemaParserLocation location, Throwable cause) {
        super(addLocation(message, location), cause);

        this.location = location;
    }

    private static String addLocation(String message, SchemaParserLocation location) {
        if (location != null) {
            message = String.format("%s (%d,%d): %s", location.getFileName(), location.getLine(), location.getColumn(), message);
        }

        return message;
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
