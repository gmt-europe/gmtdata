package nl.gmt.data.schema;

import nl.gmt.data.migrate.*;

public abstract class SchemaRules {
    public abstract String getDbType(SchemaDbType dbType) throws SchemaMigrateException;

    public int getFixedDbTypeLength(SchemaDbType dbType) {
        return -1;
    }

    public abstract boolean dbTypeSupportsSign(SchemaDbType dbType);

    public abstract boolean dbTypeSupportsLength(SchemaDbType dbType);

    public DataSchemaFactory newDataSchemaFactory() {
        return new StandardDataSchemaFactory();
    }

    public boolean areTypesEquivalent(SchemaDbType a, SchemaDbType b) {
        return a == b;
    }

    public boolean dbTypesEqual(DataSchemaField a, DataSchemaField b) throws SchemaMigrateException {
        int aArity = a.getArity() <= 0 ? 0 : a.getArity();
        int bArity = b.getArity() <= 0 ? 0 : b.getArity();

        return
            areTypesEquivalent(a.getType(), b.getType()) &&
            aArity == bArity && (
                !dbTypeSupportsSign(a.getType()) ||
                a.isUnsigned() == b.isUnsigned()
            ) && (
                !dbTypeSupportsLength(a.getType()) || (
                    (a.getPositions() == b.getPositions() || a.getPositions() == -1 || b.getPositions() == -1) &&
                    (a.getLength() == b.getLength() || a.getLength() == -1 || b.getLength() == -1)
                )
            );
    }

    public String getIndexStrategy(DataSchemaIndex index) throws SchemaMigrateException {
        if (index.getStrategy() != null)
            throw new SchemaMigrateException("Index strategy is not supported");
        return null;
    }
}
