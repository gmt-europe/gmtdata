package nl.gmt.data;

import org.apache.commons.lang.Validate;
import org.hibernate.Hibernate;
import org.hibernate.Interceptor;
import org.hibernate.bytecode.instrumentation.spi.LazyPropertyInitializer;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EntityValidator {
    private final DbConnection db;
    private final Map<EntityField, String> messages = new HashMap<>();

    public EntityValidator(DbConnection db) {
        Validate.notNull(db, "db");

        this.db = db;
    }

    public DbConnection getDb() {
        return db;
    }

    public Map<EntityField, String> getMessages() {
        return messages;
    }

    public void validate(Entity entity, EntityValidatorMode mode, boolean runInterceptors) {
        validate(entity, mode, runInterceptors, null);
    }

    public void validate(Entity entity, EntityValidatorMode mode) {
        validate(entity, mode, null);
    }

    public void validate(Entity entity, EntityValidatorMode mode, Set<String> assumeValid) {
        validate(entity, mode, false, assumeValid);
    }

    @SuppressWarnings("unchecked")
    public void validate(Entity entity, EntityValidatorMode mode, boolean runInterceptors, Set<String> assumeValid) {
        Validate.notNull(entity, "entity");
        Validate.notNull(mode, "mode");

        EntityPersister persister = (EntityPersister)db.getSessionFactory().getClassMetadata(entity.getClass());
        Object[] values = persister.getPropertyValues(entity);
        String[] propertyNames = persister.getPropertyNames();

        if (runInterceptors) {
            Interceptor interceptor = db.getSessionFactory().getSessionFactoryOptions().getInterceptor();
            Type[] propertyTypes = persister.getPropertyTypes();

            if (mode == EntityValidatorMode.UPDATE) {
                interceptor.onFlushDirty(entity, (Serializable)entity.getId(), values, values, propertyNames, propertyTypes);
            } else {
                interceptor.onSave(entity, (Serializable)entity.getId(), values, propertyNames, propertyTypes);
            }
        }

        // Check the fields.

        EntityType entityType = db.getEntitySchema().getEntityType(Hibernate.getClass(entity));

        for (int i = 0; i < values.length; i++) {
            if (assumeValid != null && assumeValid.contains(propertyNames[i])) {
                continue;
            }

            Object value = values[i];
            if (value == LazyPropertyInitializer.UNFETCHED_PROPERTY) {
                continue;
            }

            EntityField field = entityType.getField(propertyNames[i]);
            if (!(field instanceof EntityPhysicalField)) {
                continue;
            }

            EntityPhysicalField physicalField = (EntityPhysicalField)field;

            if (value == null) {
                if (!physicalField.isAllowNull()) {
                    messages.put(field, "Cannot be empty");
                }
            } else if (physicalField instanceof EntityProperty) {
                EntityProperty property = (EntityProperty)field;

                if (value instanceof String) {
                    if (property.getLength() != -1 && ((String)value).length() > property.getLength()) {
                        messages.put(field, String.format("Cannot be longer than %d", property.getLength()));
                    }
                }

                // TODO: Missing numeric validations.
            }
        }
    }
}
