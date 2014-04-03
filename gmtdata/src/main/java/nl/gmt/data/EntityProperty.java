package nl.gmt.data;

import nl.gmt.data.schema.*;
import org.apache.commons.lang.Validate;

import java.util.List;

public class EntityProperty extends EntityField {
    private final SchemaProperty schemaProperty;

    public EntityProperty(SchemaProperty schemaProperty) {
        super(schemaProperty);

        Validate.notNull(schemaProperty, "schemaProperty");

        this.schemaProperty = schemaProperty;
    }

    @Override
    void resolve(EntitySchema schema) {

    }

    public SchemaResolvedDataType getResolvedDataType() {
        return schemaProperty.getResolvedDataType();
    }

    public String getEnumType() {
        return schemaProperty.getEnumType();
    }

    public String getUserType() {
        return schemaProperty.getUserType();
    }

    public String getDbName() {
        return schemaProperty.getDbName();
    }

    public int getLength() {
        return schemaProperty.getLength();
    }

    public SchemaIndexType getIndexed() {
        return schemaProperty.getIndexed();
    }

    public String getRawDbType() {
        return schemaProperty.getRawDbType();
    }

    public SchemaDbType getDbType() {
        return schemaProperty.getDbType();
    }

    public boolean isLazy() {
        return schemaProperty.getLazy() == SchemaLazy.LAZY;
    }

    public String getResolvedDbName() {
        return schemaProperty.getResolvedDbName();
    }

    public String getRawType() {
        return schemaProperty.getRawType();
    }

    public boolean isAllowNull() {
        return schemaProperty.getAllowNull() == SchemaAllowNull.ALLOW;
    }

    public List<String> getTags() {
        return schemaProperty.getTags();
    }

    public int getPositions() {
        return schemaProperty.getPositions();
    }

    public boolean isSigned() {
        return schemaProperty.getSigned() == SchemaSigned.SIGNED;
    }

    public Class<?> getNativeType() {
        return schemaProperty.getNativeType();
    }

    public String getType() {
        return schemaProperty.getType();
    }

    public String getName() {
        return schemaProperty.getName();
    }

    public String getComments() {
        return schemaProperty.getComments();
    }
}
