package nl.gmt.data;

import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaClass;
import org.apache.commons.lang3.Validate;

import java.util.*;

public abstract class EntitySchema {
    private final Map<String, EntityType> typesByName;
    private final Map<Class<?>, EntityType> typesByClass;

    @SuppressWarnings("unchecked")
    protected EntitySchema(Schema schema) throws DataException {
        Validate.notNull(schema, "schema");

        Map<String, EntityType> typesByName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<Class<?>, EntityType> typesByClass = new HashMap<>();

        for (EntityType type : createTypes(schema)) {
            SchemaClass schemaClass = type.getSchemaClass();

            typesByName.put(schemaClass.getName(), type);
            typesByName.put(type.getModel().getSimpleName(), type);
            typesByClass.put(type.getModel(), type);
        }

        this.typesByName = Collections.unmodifiableMap(typesByName);
        this.typesByClass = Collections.unmodifiableMap(typesByClass);
    }

    protected abstract EntityType[] createTypes(Schema schema);

    public Collection<EntityType> getEntityTypes() {
        return typesByName.values();
    }

    public EntityType getEntityType(String name) {
        return getEntityType(name, true);
    }

    public EntityType getEntityType(String name, boolean throwException) {
        Validate.notNull(name, "name");

        // HACK: We should find a better solution for this.

        int pos = name.lastIndexOf('.');
        if (pos != -1) {
            name = name.substring(pos + 1);
        }

        EntityType result = typesByName.get(name);

        if (throwException) {
            Validate.notNull(result, "Entity not found");
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public EntityType getEntityType(Class<? extends Entity> klass) {
        Validate.notNull(klass, "klass");

        EntityType result = typesByClass.get(klass);

        Validate.notNull(result, "Entity not found");

        return result;
    }
}
