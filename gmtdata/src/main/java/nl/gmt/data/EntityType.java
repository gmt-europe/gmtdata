package nl.gmt.data;

import nl.gmt.data.schema.*;
import org.apache.commons.lang.Validate;

import java.lang.reflect.Method;
import java.util.*;

public class EntityType {
    private final SchemaClass schemaClass;
    private final Map<String, EntityField> fields;
    private final List<EntityIndex> indexes;
    private final Class<? extends Entity> model;

    public EntityType(Schema schema, SchemaClass schemaClass, Class<? extends Entity> model) {
        Validate.notNull(schema, "schema");
        Validate.notNull(schemaClass, "schemaClass");
        Validate.notNull(model, "model");

        this.model = model;
        this.schemaClass = schemaClass;

        Map<String, EntityField> fields = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        EntityProperty idField = new EntityProperty(schemaClass.getResolvedIdProperty(), createAccessor(schemaClass.getResolvedIdProperty()), this);

        fields.put(idField.getFieldName(), idField);

        addFields(schemaClass.getFields(), fields);

        recurseMixins(schema, schemaClass.getMixins(), fields);

        this.fields = Collections.unmodifiableMap(fields);

        this.indexes = Collections.unmodifiableList(buildIndexes(schema));
    }

    private List<EntityIndex> buildIndexes(Schema schema) {
        List<EntityIndex> indexes = new ArrayList<>();

        for (EntityField field : fields.values()) {
            SchemaIndexType indexType = field.getIndexType();

            if (indexType == SchemaIndexType.UNIQUE || indexType == SchemaIndexType.INDEX) {
                indexes.add(new EntityIndex(
                    Collections.unmodifiableList(Collections.singletonList(field)),
                    Collections.<EntityField>emptyList(),
                    indexType == SchemaIndexType.UNIQUE
                ));
            }
        }

        addIndexes(schema, indexes, schemaClass);

        return indexes;
    }

    private void addIndexes(Schema schema, List<EntityIndex> indexes, SchemaClassBase schemaClass) {
        for (String name : schemaClass.getMixins()) {
            addIndexes(schema, indexes, schema.getMixins().get(name));
        }

        for (SchemaIndex schemaIndex : schemaClass.getIndexes()) {
            List<EntityField> fields = new ArrayList<>();
            List<EntityField> includeFields = new ArrayList<>();

            for (String field : schemaIndex.getFields()) {
                fields.add(this.fields.get(field));
            }
            if (schemaIndex.getIncludeFields() != null) {
                for (String field : schemaIndex.getIncludeFields()) {
                    includeFields.add(this.fields.get(field));
                }
            }

            indexes.add(new EntityIndex(Collections.unmodifiableList(fields), Collections.unmodifiableList(includeFields), schemaIndex.getType() == SchemaIndexType.UNIQUE));
        }
    }

    private void recurseMixins(Schema schema, List<String> mixins, Map<String, EntityField> fields) {
        for (String name : mixins) {
            SchemaMixin mixin = schema.getMixins().get(name);

            addFields(mixin.getFields(), fields);

            recurseMixins(schema, mixin.getMixins(), fields);
        }
    }

    private void addFields(Collection<? extends SchemaField> schemaFields, Map<String, EntityField> fields) {
        for (SchemaField schemaField : schemaFields) {
            EntityField field;

            EntityFieldAccessor accessor = createAccessor(schemaField);

            if (schemaField instanceof SchemaProperty) {
                field = new EntityProperty((SchemaProperty)schemaField, accessor, this);
            } else if (schemaField instanceof SchemaForeignParent) {
                field = new EntityForeignParent((SchemaForeignParent)schemaField, accessor, this);
            } else {
                field = new EntityForeignChild((SchemaForeignChild)schemaField, accessor, this);
            }

            fields.put(field.getFieldName(), field);
        }
    }

    private EntityFieldAccessor createAccessor(SchemaField schemaField) {
        Method getter = null;
        Method setter = null;

        for (Method method : model.getMethods()) {
            if (
                method.getName().equals("get" + schemaField.getName()) ||
                method.getName().equals("is" + schemaField.getName())
            ) {
                getter = method;
            } else if (method.getName().equals("set" + schemaField.getName())) {
                setter = method;
            }
        }

        try {
            return EntityFieldAccessor.createAccessor(getter, setter);
        } catch (DataException e) {
            throw new RuntimeException("Cannot build entity field accessor", e);
        }
    }

    public SchemaClass getSchemaClass() {
        return schemaClass;
    }

    public EntityField getField(String name) {
        return getField(name, true);
    }

    public EntityField getField(String name, boolean throwException) {
        Validate.notNull(name, "name");

        EntityField result = fields.get(name);

        if (throwException) {
            Validate.notNull(result, "Field not found");
        }

        return result;
    }

    public Collection<EntityField> getFields() {
        return fields.values();
    }

    public List<EntityIndex> getIndexes() {
        return indexes;
    }

    @Override
    public String toString() {
        return schemaClass.getName();
    }

    public Class<? extends Entity> getModel() {
        return model;
    }

    public EntityProperty getId() {
        return (EntityProperty)getField("id");
    }
}
