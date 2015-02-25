package nl.gmt.data;

import org.apache.commons.lang.Validate;

public class DbEntityUsageRelation {
    private EntityType type;
    private int count;

    DbEntityUsageRelation(EntityType type, int count) {
        Validate.notNull(type, "type");

        this.type = type;
        this.count = count;
    }

    public EntityType getType() {
        return type;
    }

    public int getCount() {
        return count;
    }
}
