package nl.gmt.data;

import nl.gmt.data.schema.SchemaForeignChild;
import org.apache.commons.lang.Validate;

public class EntityForeignChild<T extends EntityType> extends EntityForeignBase<T> {
    private final SchemaForeignChild schemaForeignChild;
    private EntityForeignParent foreignProperty;

    public EntityForeignChild(SchemaForeignChild schemaForeignChild, EntityFieldAccessor accessor, EntityType entityType) {
        super(schemaForeignChild, accessor, entityType);

        Validate.notNull(schemaForeignChild, "schemaForeignChild");

        this.schemaForeignChild = schemaForeignChild;
    }

    @Override
    void resolve(EntitySchema schema) {
        Validate.notNull(schema, "schema");

        super.resolve(schema);

        foreignProperty = (EntityForeignParent)getForeign().getField(createFieldName(getClassProperty()));
    }

    public String getClassProperty() {
        return schemaForeignChild.getClassProperty();
    }

    public EntityForeignParent getForeignProperty() {
        return foreignProperty;
    }
}
