package nl.gmt.data.test.types;

import nl.gmt.data.EntityForeignParent;
import nl.gmt.data.EntityProperty;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaClass;

public class AddressType extends nl.gmt.data.EntityType {
    public AddressType(Schema schema, SchemaClass schemaClass) {
        super(schema, schemaClass, nl.gmt.data.test.model.Address.class);
    }

    public EntityProperty getId() {
        return (EntityProperty)getField("id");
    }

    public EntityProperty getStreet() {
        return (EntityProperty)getField("street");
    }

    public EntityProperty getHouseNumber() {
        return (EntityProperty)getField("houseNumber");
    }

    public EntityProperty getCity() {
        return (EntityProperty)getField("city");
    }

    @SuppressWarnings("unchecked")
    public EntityForeignParent<RelationType> getRelation() {
        return (EntityForeignParent<RelationType>)getField("relation");
    }
}
