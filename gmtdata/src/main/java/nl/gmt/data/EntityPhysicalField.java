package nl.gmt.data;

import nl.gmt.data.schema.SchemaIndexType;

public interface EntityPhysicalField {
    String getFieldName();

    String getDbName();

    boolean isAllowNull();

    String getResolvedDbName();

    SchemaIndexType getIndexType();
}
