package nl.gmt.data.migrate.mysql;

import nl.gmt.data.migrate.*;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaClass;
import nl.gmt.data.schema.SchemaDbType;

public class SchemaRules extends nl.gmt.data.schema.SchemaRules {
    private static final String DEFAULT_CHARSET_KEY = "DEFAULT_CHARSET";
    private static final String DEFAULT_COLLATION_KEY = "DEFAULT_COLLATION";
    private static final String ENGINE_KEY = "ENGINE";
    private static final String CHARSET_KEY = "CHARSET";
    private static final String COLLATION_KEY = "COLLATION";
    static final String DEFAULT_CHARSET = "utf8";
    static final String DEFAULT_COLLATION = "utf8_unicode_ci";
    private static final String DEFAULT_ENGINE = "innodb";

    @Override
    public String getDbType(SchemaDbType dbType) throws SchemaMigrateException {
        switch (dbType) {
            case BINARY: return "VARBINARY";
            case BLOB: return "BLOB";
            case DATE_TIME: return "DATETIME";
            case DECIMAL: return "DECIMAL";
            case DOUBLE: return "DOUBLE";
            case FIXED_BINARY: return "BINARY";
            case FIXED_STRING: return "CHAR";
            case INT: return "INT";
            case LONG_BLOB: return "LONGBLOB";
            case LONG_TEXT: return "LONGTEXT";
            case MEDIUM_BLOB: return "MEDIUMBLOB";
            case MEDIUM_TEXT: return "MEDIUMTEXT";
            case SMALL_INT: return "SMALLINT";
            case STRING: return "VARCHAR";
            case TEXT: return "TEXT";
            case TINY_BLOB: return "TINYBLOB";
            case TINY_INT: return "TINYINT";
            case TINY_TEXT: return "TINYTEXT";
            case GUID: return "BINARY";
            default: throw new SchemaMigrateException("Unexpected data type");
        }
    }

    @Override
    public boolean dbTypeSupportsSign(SchemaDbType dbType) {
        switch (dbType) {
            case INT:
            case SMALL_INT:
            case TINY_INT:
            case DOUBLE:
            case DECIMAL:
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
            case DOUBLE:
            case INT:
            case SMALL_INT:
            case TINY_INT:
            case GUID:
                return true;

            default:
                return false;
        }
    }

    public static String getDefaultCharset(DataSchemaTable table) {
        return table.getExtension(DEFAULT_CHARSET_KEY);
    }

    public static void setDefaultCharset(DataSchemaTable table, String charset) {
        table.setExtensions(DEFAULT_CHARSET_KEY, charset != null ? charset.toLowerCase() : null);
    }

    public static String getDefaultCollation(DataSchemaTable table) {
        return table.getExtension(DEFAULT_COLLATION_KEY);
    }

    public static void setDefaultCollation(DataSchemaTable table, String collation) {
        table.setExtensions(DEFAULT_COLLATION_KEY, collation != null ? collation.toLowerCase() : null);
    }

    public static String getEngine(DataSchemaTable table) {
        return table.getExtension(ENGINE_KEY);
    }

    public static void setEngine(DataSchemaTable table, String engine) {
        table.setExtensions(ENGINE_KEY, engine != null ? engine.toLowerCase() : null);
    }

    public static String getCharset(DataSchemaField field) {
        return field.getExtension(CHARSET_KEY);
    }

    public static void setCharset(DataSchemaField field, String charset) {
        field.setExtensions(CHARSET_KEY, charset != null ? charset.toLowerCase() : null);
    }

    public static String getCollation(DataSchemaField field) {
        return field.getExtension(COLLATION_KEY);
    }

    public static void setCollation(DataSchemaField field, String collation) {
        field.setExtensions(COLLATION_KEY, collation != null ? collation.toLowerCase() : null);
    }

    static boolean dbTypeSupportsCharset(SchemaDbType dbType) {
        switch (dbType) {
            case LONG_TEXT:
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
    public DataSchemaFactory newDataSchemaFactory() {
        return new StandardDataSchemaFactory() {
            @Override
            public DataSchemaTable createClass(SchemaClass klass, Schema schema, nl.gmt.data.schema.SchemaRules rules) throws SchemaMigrateException {
                DataSchemaTable result = super.createClass(klass, schema, rules);

                setDefaultCharset(result, DEFAULT_CHARSET);
                setDefaultCollation(result, DEFAULT_COLLATION);
                setEngine(result, DEFAULT_ENGINE);

                for (DataSchemaField field : result.getFields().values()) {
                    if (dbTypeSupportsCharset(field.getType())) {
                        setCharset(field, DEFAULT_CHARSET);
                        setCollation(field, DEFAULT_COLLATION);
                    }
                }

                return result;
            }
        };
    }

    @Override
    public int getFixedDbTypeLength(SchemaDbType dbType) {
        switch (dbType) {
            case GUID: return 16;
            default: return super.getFixedDbTypeLength(dbType);
        }
    }

    @Override
    public boolean dbTypesEqual(DataSchemaField a, DataSchemaField b) throws SchemaMigrateException {
        // If any of the types are GUID, the other type matches if its a CHAR(36).

        return
            ((a.getType() == SchemaDbType.GUID || b.getType() == SchemaDbType.GUID) && isLikeGuid(a) && isLikeGuid(b)) ||
            super.dbTypesEqual(a, b);
    }

    private boolean isLikeGuid(DataSchemaField field) {
        return
            field.getType() == SchemaDbType.GUID ||
            (field.getType() == SchemaDbType.FIXED_BINARY && field.getLength() == 16);
    }
}
