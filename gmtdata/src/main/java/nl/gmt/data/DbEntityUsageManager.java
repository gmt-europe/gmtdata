package nl.gmt.data;

import org.apache.commons.lang.Validate;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import java.util.*;

class DbEntityUsageManager<T extends EntitySchema> {
    private static final DbEntityUsage EMPTY_USAGE = new DbEntityUsage(Collections.<DbEntityUsageRelation>emptyList());

    private final DbConnection<T> db;
    private final Map<EntityType, List<Relationship>> relationships = new HashMap<>();

    public DbEntityUsageManager(DbConnection<T> db) {
        Validate.notNull(db, "db");

        this.db = db;

        // Build an inverse map of all relationships.

        for (EntityType type : db.getEntitySchema().getEntityTypes()) {
            for (EntityField field : type.getFields()) {
                if (!(field instanceof EntityForeignParent)) {
                    continue;
                }

                EntityForeignParent foreignParent = (EntityForeignParent)field;

                List<Relationship> relationships = this.relationships.get(foreignParent.getForeign());
                if (relationships == null) {
                    relationships = new ArrayList<>();
                    this.relationships.put(foreignParent.getForeign(), relationships);
                }

                Relationship relationship = null;
                for (Relationship item : relationships) {
                    if (item.type == type) {
                        relationship = item;
                        break;
                    }
                }

                if (relationship == null) {
                    relationship = new Relationship(type);
                    relationships.add(relationship);
                }

                relationship.fields.add(foreignParent);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public DbEntityUsage getUsage(DbContext ctx, Entity entity, EntityType... exclusions) {
        Validate.notNull(ctx, "ctx");
        Validate.notNull(entity, "entity");

        EntityType type = db.getEntitySchema().getEntityType((Class<? extends Entity>)Hibernate.getClass(entity));

        List<Relationship> relationships = this.relationships.get(type);
        if (relationships == null) {
            return EMPTY_USAGE;
        }

        List<DbEntityUsageRelation> usageRelations = null;

        for (Relationship relationship : relationships) {
            if (isExcluded(relationship, exclusions)) {
                continue;
            }

            Criteria criteria = ctx.createCriteria(relationship.type.getModel());

            // Exclude the current entity if the relationship is of this entity.

            if (type == relationship.type) {
                criteria.add(Restrictions.ne(type.getId().getFieldName(), entity.getId()));
            }

            // Build an or-ed filter of all dependent fields.

            Criterion criterion = null;

            for (EntityForeignParent field : relationship.fields) {
                Criterion restriction = Restrictions.eq(field.getFieldName(), entity);
                if (criterion != null) {
                    restriction = Restrictions.or(criterion, restriction);
                }
                criterion = restriction;
            }

            criteria.add(criterion);

            // Get the number of matched rows.

            int count = ((Number)criteria.setProjection(Projections.rowCount()).uniqueResult()).intValue();

            if (count > 0) {
                if (usageRelations == null) {
                    usageRelations = new ArrayList<>();
                }

                usageRelations.add(new DbEntityUsageRelation(relationship.type, count));
            }
        }

        if (usageRelations == null) {
            return EMPTY_USAGE;
        }

        return new DbEntityUsage(usageRelations);
    }

    private boolean isExcluded(Relationship relationship, EntityType[] exclusions) {
        if (exclusions == null || exclusions.length == 0) {
            return false;
        }

        for (EntityType exclusion : exclusions) {
            if (exclusion == relationship.type) {
                return true;
            }
        }

        return false;
    }

    private static class Relationship {
        final EntityType type;
        final List<EntityForeignParent> fields = new ArrayList<>();

        public Relationship(EntityType type) {
            this.type = type;
        }
    }
}
