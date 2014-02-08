package nl.gmt.data.migrate;

import nl.gmt.data.schema.SchemaParserLocation;

public class SchemaMigrateException extends Exception {
    private SchemaParserLocation location;

    public SchemaMigrateException() {
    }

    public SchemaMigrateException(String message) {
        super(message);
    }

    public SchemaMigrateException(String message, Throwable cause) {
        super(message, cause);
    }

    public SchemaMigrateException(String message, SchemaParserLocation location) {
        super(message);

        this.location = location;
    }

    public SchemaMigrateException(String message, SchemaParserLocation location, Throwable cause) {
        super(message, cause);

        this.location = location;
    }

    public SchemaMigrateException(Throwable cause) {
        super(cause);
    }

    public SchemaMigrateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SchemaParserLocation getLocation() {
        return location;
    }
}
