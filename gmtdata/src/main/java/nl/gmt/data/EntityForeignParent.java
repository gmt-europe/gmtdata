package nl.gmt.data;

import nl.gmt.data.schema.SchemaAllowNull;
import nl.gmt.data.schema.SchemaForeignParent;
import nl.gmt.data.schema.SchemaIndexType;
import org.apache.commons.lang.Validate;

public class EntityForeignParent<T extends EntityType> extends EntityForeignBase<T> implements EntityPhysicalField {
    private final SchemaForeignParent schemaForeignParent;

    public EntityForeignParent(SchemaForeignParent schemaForeignParent) {
        super(schemaForeignParent);

        Validate.notNull(schemaForeignParent, "schemaForeignParent");
        
        this.schemaForeignParent = schemaForeignParent;
    }

    @Override
    public String getDbName() {
        return schemaForeignParent.getDbName();
    }

    @Override
    public boolean isAllowNull() {
        return schemaForeignParent.getAllowNull() ==  SchemaAllowNull.ALLOW;
    }

    @Override
    public String getResolvedDbName() {
        return schemaForeignParent.getResolvedDbName();
    }

    @Override
    public SchemaIndexType getIndexType() {
        return schemaForeignParent.getIndexType();
    }
}
