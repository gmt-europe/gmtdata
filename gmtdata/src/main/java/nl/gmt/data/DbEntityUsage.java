package nl.gmt.data;

import org.apache.commons.lang.Validate;

import java.util.Collections;
import java.util.List;

public class DbEntityUsage {
    private final int count;
    private final List<DbEntityUsageRelation> relations;

    DbEntityUsage(List<DbEntityUsageRelation> relations) {
        Validate.notNull(relations, "relations");

        int count = 0;

        for (DbEntityUsageRelation relation : relations) {
            count += relation.getCount();
        }

        this.count = count;
        this.relations = Collections.unmodifiableList(relations);
    }

    public int getCount() {
        return count;
    }

    public List<DbEntityUsageRelation> getRelations() {
        return relations;
    }
}
