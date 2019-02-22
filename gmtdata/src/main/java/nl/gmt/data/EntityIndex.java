package nl.gmt.data;

import org.apache.commons.lang.Validate;

import java.util.List;

public class EntityIndex {
    private final List<EntityField> fields;
    private final List<EntityField> includeFields;
    private final boolean unique;

    EntityIndex(List<EntityField> fields, List<EntityField> includeFields, boolean unique) {
        Validate.notNull(fields, "fields");
        Validate.notNull(includeFields, "includeFields");

        this.fields = fields;
        this.includeFields = includeFields;
        this.unique = unique;
    }

    public List<EntityField> getFields() {
        return fields;
    }

    public List<EntityField> getIncludeFields() {
        return includeFields;
    }

    public boolean isUnique() {
        return unique;
    }
}
