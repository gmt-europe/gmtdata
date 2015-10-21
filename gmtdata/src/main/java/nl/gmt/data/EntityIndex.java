package nl.gmt.data;

import org.apache.commons.lang.Validate;

import java.util.List;

public class EntityIndex {
    private final List<EntityField> fields;
    private final boolean unique;

    EntityIndex(List<EntityField> fields, boolean unique) {
        Validate.notNull(fields, "fields");

        this.fields = fields;
        this.unique = unique;
    }

    public List<EntityField> getFields() {
        return fields;
    }

    public boolean isUnique() {
        return unique;
    }
}
