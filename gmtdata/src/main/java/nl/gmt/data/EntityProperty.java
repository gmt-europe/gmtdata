package nl.gmt.data;

import nl.gmt.data.schema.*;
import org.apache.commons.lang3.Validate;

import java.util.List;

public class EntityProperty extends EntityField implements EntityPhysicalField {
    private final SchemaProperty schemaProperty;
    private final SchemaResolvedDataType resolvedType;

    public EntityProperty(SchemaProperty schemaProperty) {
        super(schemaProperty);

        Validate.notNull(schemaProperty, "schemaProperty");

        this.schemaProperty = schemaProperty;
        resolvedType = schemaProperty.getResolvedDataType();
    }

    @Override
    void resolve(EntitySchema schema) {

    }

    public String getEnumType() {
        return resolvedType.getEnumType();
    }

    public String getUserType() {
        return resolvedType.getUserType();
    }

    @Override
    public String getDbName() {
        return schemaProperty.getDbName();
    }

    public int getLength() {
        return resolvedType.getLength();
    }

    @Override
    public SchemaIndexType getIndexType() {
        return resolvedType.getIndexType();
    }

    public SchemaDbType getDbType() {
        return resolvedType.getDbType();
    }

    public boolean isLazy() {
        return resolvedType.getLazy() == SchemaLazy.LAZY;
    }

    @Override
    public String getResolvedDbName() {
        return schemaProperty.getResolvedDbName();
    }

    @Override
    public boolean isAllowNull() {
        return resolvedType.getAllowNull() == SchemaAllowNull.ALLOW;
    }

    public List<String> getTags() {
        return schemaProperty.getTags();
    }

    public int getPositions() {
        return resolvedType.getPositions();
    }

    public boolean isSigned() {
        return resolvedType.getSigned() == SchemaSigned.SIGNED;
    }

    public Class<?> getNativeType() {
        return resolvedType.getNativeType();
    }

    public String getType() {
        return resolvedType.getType();
    }

    public String getName() {
        return schemaProperty.getName();
    }

    public String getComments() {
        return schemaProperty.getComments();
    }
}
