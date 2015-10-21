package nl.gmt.data;

import org.apache.commons.lang.Validate;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.Interceptor;
import org.hibernate.bytecode.instrumentation.spi.LazyPropertyInitializer;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EntityValidator {
    private final DbContext ctx;
    private final Map<EntityField, String> messages = new HashMap<>();

    public EntityValidator(DbContext ctx) {
        Validate.notNull(ctx, "ctx");

        this.ctx = ctx;
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

        DbConnection db = ctx.getConnection();

        EntityPersister persister = (EntityPersister)db.getSessionFactory().getClassMetadata(Hibernate.getClass(entity));
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
                    messages.put(field, db.getText("gmtdata.validation.cannot-be-empty"));
                }
            } else if (physicalField instanceof EntityProperty) {
                EntityProperty property = (EntityProperty)field;

                if (value instanceof String) {
                    if (property.getLength() != -1 && ((String)value).length() > property.getLength()) {
                        messages.put(field, db.getText("gmtdata.validation.cannot-be-longer-than", property.getLength()));
                    }
                }

                // TODO: Missing numeric validations.
            }
        }

        // Check unique indexes. The unique check is expensive, so skip it if we already have errors.

        if (messages.size() == 0) {
            boolean skipNullInUniqueIndex = db.getDriver().skipNullInUniqueIndex();

            FlushMode flushMode = null;

            for (EntityIndex index : entityType.getIndexes()) {
                if (!index.isUnique()) {
                    continue;
                }

                List<EntityField> fields = index.getFields();

                // Do we have any null values?

                Object[] indexValues = new Object[fields.size()];
                boolean haveNull = false;

                for (int i = 0; i < fields.size(); i++) {
                    EntityField field = fields.get(i);

                    // Find the value of the field.

                    int fieldIndex = -1;
                    for (int j = 0; j < propertyNames.length; j++) {
                        if (propertyNames[j].equals(field.getFieldName())) {
                            fieldIndex = j;
                            break;
                        }
                    }

                    if (fieldIndex == -1) {
                        throw new IllegalStateException("Cannot find property");
                    }

                    Object value = values[fieldIndex];
                    indexValues[i] = value;

                    if (value == null) {
                        haveNull = true;
                    }
                }

                // If any of the values are null, and we need to skip unique indexes with null values,
                // suppress the check.

                if (skipNullInUniqueIndex && haveNull) {
                    continue;
                }

                // Because we're doing a query, we need to temporarily disable auto flush. Here we set the flush
                // mode to manual and restore it after we've done our checks. This may hide a few updates from us,
                // but it's (probably) the best we can do. An alternative would be to temporarily evict the entity,
                // but the side effects of that aren't pretty.

                if (flushMode == null) {
                    flushMode = ctx.getSession().getFlushMode();
                    ctx.getSession().setFlushMode(FlushMode.MANUAL);
                }

                // Build a query to count the number of entities that have unique index combination.

                Criteria criteria = ctx.createCriteria(entityType.getModel());

                for (int i = 0; i < fields.size(); i++) {
                    EntityField field = fields.get(i);

                    Object value = indexValues[i];

                    if (value == null) {
                        criteria.add(Restrictions.isNull(field.getFieldName()));
                    } else {
                        criteria.add(Restrictions.eq(field.getFieldName(), value));
                    }
                }

                // Ignore the current entity when we're updating.

                if (mode == EntityValidatorMode.UPDATE) {
                    criteria.add(Restrictions.not(Restrictions.eq(entityType.getId().getFieldName(), entity.getId())));
                }

                criteria.setProjection(Projections.rowCount());

                int rowCount = ((Number)criteria.uniqueResult()).intValue();

                if (rowCount > 0) {
                    // We add a unique index message on all fields of the unique index.

                    for (EntityField field : fields) {
                        messages.put(field, db.getText("gmtdata.validation.unique-index-conflict"));
                    }
                }
            }

            // Restore the flush mode if we changed it.

            if (flushMode != null) {
                ctx.getSession().setFlushMode(flushMode);
            }
        }
    }
}
