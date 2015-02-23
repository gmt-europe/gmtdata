package nl.gmt.data.test.types;

import nl.gmt.data.DataException;
import nl.gmt.data.EntityType;
import nl.gmt.data.schema.Schema;

public class EntitySchema extends nl.gmt.data.EntitySchema {
    private AddressType address;
    private RelationType relation;

    public EntitySchema(Schema schema) throws DataException {
        super(schema);
    }

    @Override
    protected EntityType[] createTypes(Schema schema) {
        return new EntityType[]{
            address = new AddressType(schema, schema.getClasses().get("Address")),
            relation = new RelationType(schema, schema.getClasses().get("Relation"))
        };
    }

    public AddressType getAddress() {
        return address;
    }

    public RelationType getRelation() {
        return relation;
    }
}
