package nl.gmt.data;

import nl.gmt.data.schema.SchemaField;
import org.apache.commons.lang3.Validate;

public abstract class EntityField {
    private final SchemaField field;
    private final String fieldName;
    private final EntityFieldAccessor accessor;
    private final EntityType entityType;

    protected EntityField(SchemaField field, EntityFieldAccessor accessor, EntityType entityType) {
        Validate.notNull(field, "field");
        Validate.notNull(accessor, "accessor");
        Validate.notNull(entityType, "entityType");

        this.field = field;
        this.fieldName = createFieldName(field.getName());
        this.accessor = accessor;
        this.entityType = entityType;
    }

    static String createFieldName(String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    abstract void resolve(EntitySchema schema);

    public String getName() {
        return field.getName();
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String toString() {
        return fieldName;
    }

    public Object getValue(Entity entity) {
        Validate.notNull(entity, "entity");

        return accessor.getValue(entity);
    }

    public void setValue(Entity entity, Object value) {
        Validate.notNull(entity, "entity");

        accessor.setValue(entity, value);
    }

    public EntityType getEntityType() {
        return entityType;
    }
}
