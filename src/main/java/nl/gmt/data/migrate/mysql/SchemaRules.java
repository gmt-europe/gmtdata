package nl.gmt.data.migrate.mysql;

import nl.gmt.data.migrate.DataSchemaField;
import nl.gmt.data.migrate.SchemaMigrateException;
import nl.gmt.data.schema.SchemaDbType;

public class SchemaRules extends nl.gmt.data.schema.SchemaRules {
    @Override
    public String getDbType(SchemaDbType dbType) throws SchemaMigrateException {
        switch (dbType) {
            case BIG_INT: return "BIGINT";
            case BINARY: return "VARBINARY";
            case BLOB: return "BLOB";
            case DATE: return "DATE";
            case DATE_TIME: return "DATETIME";
            case DECIMAL: return "DECIMAL";
            case DOUBLE: return "DOUBLE";
            case ENUMERATION: return "ENUM";
            case FIXED_BINARY: return "BINARY";
            case FIXED_STRING: return "CHAR";
            case FLOAT: return "FLOAT";
            case INT: return "INT";
            case LONG_BLOB: return "LONGBLOB";
            case LONG_TEXT: return "LONGTEXT";
            case MEDIUM_BLOB: return "MEDIUMBLOB";
            case MEDIUM_INT: return "MEDIUMINT";
            case MEDIUM_TEXT: return "MEDIUMTEXT";
            case SMALL_INT: return "SMALLINT";
            case STRING: return "VARCHAR";
            case TEXT: return "TEXT";
            case TIME: return "TIME";
            case TIMESTAMP: return "TIMESTAMP";
            case TINY_BLOB: return "TINYBLOB";
            case TINY_INT: return "TINYINT";
            case TINY_TEXT: return "TINYTEXT";
            case YEAR: return "YEAR";
            case GUID: return "CHAR";
            default: throw new SchemaMigrateException("Unexpected data type");
        }
    }

    @Override
    public boolean dbTypeSupportsSign(SchemaDbType dbType) {
        switch (dbType) {
            case INT:
            case SMALL_INT:
            case TINY_INT:
            case MEDIUM_INT:
            case BIG_INT:
            case DOUBLE:
            case FLOAT:
            case DECIMAL:
                return true;

            default:
                return false;
        }
    }

    @Override
    public boolean dbTypeSupportsCharset(SchemaDbType dbType) {
        switch (dbType) {
            case LONG_TEXT:
            case ENUMERATION:
            case STRING:
            case TINY_TEXT:
            case TEXT:
            case MEDIUM_TEXT:
            case FIXED_STRING:
                return true;

            default:
                return false;
        }
    }

    @Override
    public boolean dbTypeSupportsLength(SchemaDbType dbType) {
        switch (dbType) {
            case STRING:
            case FIXED_STRING:
            case FIXED_BINARY:
            case BINARY:
            case DECIMAL:
            case FLOAT:
            case DOUBLE:
            case INT:
            case SMALL_INT:
            case BIG_INT:
            case TINY_INT:
            case MEDIUM_INT:
            case GUID:
                return true;

            default:
                return false;
        }
    }

    @Override
    public boolean supportsCharset() {
        return true;
    }

    @Override
    public String getDefaultCharset() {
        return "utf8";
    }

    @Override
    public String getDefaultCollation() {
        return "utf8_unicode_ci";
    }

    @Override
    public boolean supportsEngine() {
        return true;
    }

    @Override
    public String getDefaultEngine() {
        return "InnoDB";
    }

    @Override
    public int getFixedDbTypeLength(SchemaDbType dbType) {
        switch (dbType) {
            case GUID: return 36;
            default: return super.getFixedDbTypeLength(dbType);
        }
    }

    @Override
    public boolean dbTypesEqual(DataSchemaField a, DataSchemaField b) throws SchemaMigrateException {
        if (
            (
                a.getType() == SchemaDbType.GUID &&
                (b.getType() == SchemaDbType.FIXED_STRING && b.getLength() == 36)
            ) ||
            (
                b.getType() == SchemaDbType.GUID &&
                (a.getType() == SchemaDbType.FIXED_STRING && b.getLength() == 36)
            )
        ) {
            return true;
        } else {
            return super.dbTypesEqual(a, b);
        }
    }
}
