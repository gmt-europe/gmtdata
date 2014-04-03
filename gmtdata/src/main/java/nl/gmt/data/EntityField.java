package nl.gmt.data;

import nl.gmt.data.schema.SchemaField;
import org.apache.commons.lang.Validate;

public abstract class EntityField {
    private final String fieldName;

    protected EntityField(SchemaField schemaField) {
        Validate.notNull(schemaField, "schemaField");

        fieldName = createFieldName(schemaField.getName());
    }

    static String createFieldName(String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    abstract void resolve(EntitySchema schema);

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String toString() {
        return fieldName;
    }
}
