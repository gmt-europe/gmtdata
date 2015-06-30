package nl.gmt.data.migrate.postgres;

import nl.gmt.data.migrate.SchemaMigrateException;
import nl.gmt.data.schema.SchemaDbType;

public class SchemaRules extends nl.gmt.data.schema.SchemaRules {
    @Override
    public String getDbType(SchemaDbType dbType) throws SchemaMigrateException {
        switch (dbType) {
            case BINARY_JSON:
                return "JSONB";

            case BINARY:
            case BLOB:
            case LONG_BLOB:
            case MEDIUM_BLOB:
            case TINY_BLOB:
                return "BYTEA";

            case CASE_INSENSITIVE_TEXT:
                return "CITEXT";

            case DATE_TIME:
                return "TIMESTAMP";

            case DECIMAL:
                return "NUMERIC";

            case DOUBLE:
                return "DOUBLE PRECISION";

            case FIXED_STRING:
                return "CHARACTER";

            case GUID:
                return "UUID";

            case INT:
                return "INTEGER";

            case JSON:
                return "JSON";

            case SMALL_INT:
                return "SMALLINT";

            case STRING:
                return "CHARACTER VARYING";

            case LONG_TEXT:
            case MEDIUM_TEXT:
            case TEXT:
            case TINY_TEXT:
                return "TEXT";

            case TINY_INT:
                return "BOOLEAN";

            default: throw new SchemaMigrateException("Unexpected data type");
        }
    }

    @Override
    public boolean dbTypeSupportsSign(SchemaDbType dbType) {
        return false;
    }

    @Override
    public boolean dbTypeSupportsLength(SchemaDbType dbType) {
        switch (dbType) {
            case DATE_TIME:
            case DECIMAL:
            case STRING:
                return true;

            default:
                return false;
        }
    }

    @Override
    public String getIndexStrategy(String strategy) throws SchemaMigrateException {
        if (strategy == null)
            return "btree";
        return strategy.toLowerCase();
    }
}
