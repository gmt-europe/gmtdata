package nl.gmt.data.test.model.implementation;

import nl.gmt.data.hibernate.HibernateRepository;
import nl.gmt.data.test.model.Address;
import nl.gmt.data.test.model.AddressRepository;
import nl.gmt.data.test.model.Relation;
import org.apache.commons.lang.Validate;

public class AddressRepositoryImpl extends HibernateRepository<Address> implements AddressRepository {
    public AddressRepositoryImpl() {
        super(Address.class);
    }

    @Override
    public Address findByStreetAndHouseNumberAndCityAndRelation(String street, int houseNumber, String city, Relation relation) {
        Validate.notNull(street, "street");
        Validate.notNull(city, "city");
        Validate.notNull(relation, "relation");

        return this.<Address>createQuery("from Address a where a.street = :street and a.houseNumber = :houseNumber and a.city = :city and a.relation = :relation")
            .setString("street", street)
            .setInteger("houseNumber", houseNumber)
            .setString("city", city)
            .setEntity("relation", relation)
            .uniqueResult();
    }
}
