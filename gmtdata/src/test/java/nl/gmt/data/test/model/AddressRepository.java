package nl.gmt.data.test.model;

import nl.gmt.data.Repository;

public interface AddressRepository extends Repository<Address> {
    Address findByStreetAndHouseNumberAndCityAndRelation(String street, int houseNumber, String city, Relation relation);
}
