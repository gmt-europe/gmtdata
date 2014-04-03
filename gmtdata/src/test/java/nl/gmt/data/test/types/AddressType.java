package nl.gmt.data.test.types;

import nl.gmt.data.EntityForeignParent;
import nl.gmt.data.EntityProperty;
import nl.gmt.data.schema.SchemaClass;

public class AddressType extends nl.gmt.data.EntityType {
    public AddressType(SchemaClass schemaClass) {
        super(schemaClass);
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