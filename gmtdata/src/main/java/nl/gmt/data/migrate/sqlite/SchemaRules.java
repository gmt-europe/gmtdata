package nl.gmt.data.migrate.sqlite;

import nl.gmt.data.migrate.DataSchemaField;
import nl.gmt.data.migrate.SchemaMigrateException;
import nl.gmt.data.schema.SchemaDbType;

public class SchemaRules extends nl.gmt.data.schema.SchemaRules {
    @Override
    public String getDbType(SchemaDbType dbType) throws SchemaMigrateException {
        switch (simplifyType(dbType)) {
            case INT: return "INT";
            case SMALL_INT: return "SMALLINT";
            case TINY_INT: return "TINYINT";
            case BLOB: return "BLOB";
            case TEXT: return "TEXT";
            case DOUBLE: return "DOUBLE";
            case DECIMAL: return "DECIMAL";
            case DATE_TIME: return "DATETIME";
            case GUID: return "GUID";
            default: throw new SchemaMigrateException("Unexpected data type");
        }
    }

    @Override
    public boolean dbTypeSupportsSign(SchemaDbType dbType) {
        return false;
    }

    @Override
    public boolean dbTypeSupportsLength(SchemaDbType dbType) {
        return false;
    }

    @Override
    public boolean dbTypesEqual(DataSchemaField a, DataSchemaField b) throws SchemaMigrateException {
        // SQLite does not look at lengths, positions or scale. This
        // means that we only have to compare the data types
        // as described by the affinity rules.

        return simplifyType(a.getType()) == simplifyType(b.getType());
    }

    public static SchemaDbType simplifyType(SchemaDbType type) throws SchemaMigrateException {
        // Below are the type rules for SQLite's implementation of
        // ADO.Net as taken from http://sqlite.phxsoftware.com/forums/t/31.aspx.

        switch (type) {
            case BINARY:
            case BLOB:
            case FIXED_BINARY:
            case LONG_BLOB:
            case MEDIUM_BLOB:
            case TINY_BLOB:
                return SchemaDbType.BLOB;

            case FIXED_STRING:
            case LONG_TEXT:
            case MEDIUM_TEXT:
            case STRING:
            case TEXT:
            case TINY_TEXT:
                return SchemaDbType.TEXT;

            default:
                return type;
        }
    }
}
