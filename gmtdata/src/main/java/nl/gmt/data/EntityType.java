package nl.gmt.data;

import nl.gmt.data.schema.*;
import org.apache.commons.lang.Validate;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EntityType {
    private final SchemaClass schemaClass;
    private final Map<String, EntityField> fields;
    private final Class<?> model;

    public EntityType(SchemaClass schemaClass, Class<?> model) {
        this.model = model;
        Validate.notNull(schemaClass, "schemaClass");
        Validate.notNull(model, "model");

        this.schemaClass = schemaClass;

        Map<String, EntityField> fields = new HashMap<>();

        for (SchemaField schemaField : schemaClass.getFields()) {
            EntityField field;

            if (schemaField instanceof SchemaProperty) {
                field = new EntityProperty((SchemaProperty)schemaField);
            } else if (schemaField instanceof SchemaForeignParent) {
                field = new EntityForeignParent((SchemaForeignParent)schemaField);
            } else {
                field = new EntityForeignChild((SchemaForeignChild)schemaField);
            }

            fields.put(field.getFieldName(), field);
        }

        this.fields = Collections.unmodifiableMap(fields);
    }

    public SchemaClass getSchemaClass() {
        return schemaClass;
    }

    public EntityField getField(String name) {
        Validate.notNull(name, "name");

        EntityField result = fields.get(name);

        Validate.notNull(result, "Field not found");

        return result;
    }

    public Collection<EntityField> getFields() {
        return fields.values();
    }

    @Override
    public String toString() {
        return schemaClass.getName();
    }

    public Class<?> getModel() {
        return model;
    }
}
