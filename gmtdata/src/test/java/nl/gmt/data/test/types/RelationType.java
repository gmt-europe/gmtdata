package nl.gmt.data.test.types;

import nl.gmt.data.EntityForeignChild;
import nl.gmt.data.EntityProperty;
import nl.gmt.data.EntityType;
import nl.gmt.data.schema.SchemaClass;

public class RelationType extends EntityType {
    public RelationType(SchemaClass schemaClass) {
        super(schemaClass);
    }

    public EntityProperty getId() {
        return (EntityProperty)getField("id");
    }

    public EntityProperty getPicture() {
        return (EntityProperty)getField("picture");
    }

    public EntityProperty getName() {
        return (EntityProperty)getField("name");
    }

    public EntityProperty getGender() {
        return (EntityProperty)getField("gender");
    }

    @SuppressWarnings("unchecked")
    public EntityForeignChild<AddressType> getAddresses() {
        return (EntityForeignChild<AddressType>)getField("addresses");
    }
}
