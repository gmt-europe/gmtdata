package nl.gmt.data;

import nl.gmt.data.schema.SchemaForeignBase;
import nl.gmt.data.schema.SchemaForeignType;
import org.apache.commons.lang.Validate;

import java.util.List;

public abstract class EntityForeignBase<T extends EntityType> extends EntityField {
    private final SchemaForeignBase schemaForeignBase;
    private EntityType foreign;

    protected EntityForeignBase(SchemaForeignBase schemaForeignBase) {
        super(schemaForeignBase);

        Validate.notNull(schemaForeignBase, "schemaForeignBase");

        this.schemaForeignBase = schemaForeignBase;
    }

    @Override
    void resolve(EntitySchema schema) {
        Validate.notNull(schema, "schema");

        foreign = schema.getEntityType(getClassName());
    }

    public SchemaForeignType getType() {
        return schemaForeignBase.getType();
    }

    public String getName() {
        return schemaForeignBase.getName();
    }

    public List<String> getTags() {
        return schemaForeignBase.getTags();
    }

    public String getComments() {
        return schemaForeignBase.getComments();
    }

    public String getClassName() {
        return schemaForeignBase.getClassName();
    }

    @SuppressWarnings("unchecked")
    public T getForeign() {
        return (T)foreign;
    }
}
