package nl.gmt.data.migrate.sqlserver;

import nl.gmt.data.migrate.DataSchemaField;
import nl.gmt.data.migrate.DataSchemaIndex;
import nl.gmt.data.migrate.SchemaMigrateException;
import nl.gmt.data.schema.SchemaDbType;
import nl.gmt.data.schema.SchemaIndexType;

public class SchemaRules extends nl.gmt.data.schema.SchemaRules {
    @Override
    public String getDbType(SchemaDbType dbType) throws SchemaMigrateException {
        switch (dbType) {
            case FIXED_BINARY: return "BINARY";
            case DATE_TIME: return "DATETIME2";
            case DECIMAL: return "DECIMAL";
            case INT: return "INT";
            case CASE_INSENSITIVE_TEXT: return "NCHAR";
            case STRING: return "NVARCHAR";
            case DOUBLE: return "REAL";
            case SMALL_INT: return "SMALLINT";
            case TINY_INT: return "TINYINT";
            case GUID: return "UNIQUEIDENTIFIER";
            case BINARY: return "VARBINARY";
            case TEXT:
            case LONG_TEXT:
                return "NTEXT";
            case BLOB:
            case LONG_BLOB:
                return "IMAGE";

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
            case DECIMAL:
            case BINARY:
            case FIXED_BINARY:
            case STRING:
            case FIXED_STRING:
                return true;

            default:
                return false;
        }
    }

    @Override
    public String getIndexStrategy(DataSchemaIndex index) throws SchemaMigrateException {
        if (index.getStrategy() == null) {
            if (index.getType() == SchemaIndexType.PRIMARY) {
                return "CLUSTERED";
            }
            return "NONCLUSTERED";
        }
        return index.getStrategy().toUpperCase();
    }

    @Override
    public boolean areTypesEquivalent(SchemaDbType a, SchemaDbType b) {
        return super.areTypesEquivalent(simplifyType(a), simplifyType(b));
    }

    private SchemaDbType simplifyType(SchemaDbType type) {
        switch (type) {
            case LONG_TEXT:
                return SchemaDbType.TEXT;
            case LONG_BLOB:
                return SchemaDbType.BLOB;
            default:
                return type;
        }
    }

    @Override
    public boolean dbTypesEqual(DataSchemaField a, DataSchemaField b) throws SchemaMigrateException {
        if (a.getType() == b.getType() && (a.getType() == SchemaDbType.STRING || a.getType() == SchemaDbType.BINARY)) {
            switch (a.getType()) {
                case STRING:
                case BINARY:
                    return a.getLength() == b.getLength();
                case DECIMAL:
                    return a.getLength() == b.getLength() && a.getPositions() == b.getPositions();
            }
        }

        return super.dbTypesEqual(a, b);
    }
}
