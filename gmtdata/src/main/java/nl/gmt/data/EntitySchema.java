package nl.gmt.data;

import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaClass;
import org.apache.commons.lang.Validate;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class EntitySchema {
    private final Map<String, EntityType> typesByName;
    private final Map<Class<?>, EntityType> typesByClass;

    @SuppressWarnings("unchecked")
    protected EntitySchema(Schema schema) throws DataException {
        Validate.notNull(schema, "schema");

        Map<String, EntityType> typesByName = new HashMap<>();
        Map<Class<?>, EntityType> typesByClass = new HashMap<>();

        try {
            for (EntityType type : createTypes(schema)) {
                SchemaClass schemaClass = type.getSchemaClass();

                typesByName.put(schemaClass.getName(), type);

                String className = schema.getNamespace() + ".model";

                if (schemaClass.getBoundedContext() != null) {
                    className += "." + schemaClass.getBoundedContext();
                }

                typesByClass.put(Class.forName(className + "." + schemaClass.getName()), type);
            }
        } catch (ClassNotFoundException e) {
            throw new DataException("Cannot load schema", e);
        }

        this.typesByName = Collections.unmodifiableMap(typesByName);
        this.typesByClass = Collections.unmodifiableMap(typesByClass);
    }

    protected abstract EntityType[] createTypes(Schema schema);

    public Collection<EntityType> getEntityTypes() {
        return typesByName.values();
    }

    public EntityType getEntityType(String name) {
        Validate.notNull(name, "name");

        EntityType result = typesByName.get(name);

        Validate.notNull(result, "Entity not found");

        return result;
    }

    public EntityType getEntityType(Class<?> klass) {
        Validate.notNull(klass, "klass");

        EntityType result = typesByClass.get(klass);

        Validate.notNull(result, "Entity not found");

        return result;
    }
}
