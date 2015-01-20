package nl.gmt.data.module2.model.implementation;

import nl.gmt.data.hibernate.HibernateRepository;

import nl.gmt.data.module2.model.Address;
import nl.gmt.data.module2.model.AddressRepository;

@SuppressWarnings("UnusedDeclaration")
public class AddressRepositoryImpl extends HibernateRepository<Address> implements AddressRepository {
    public AddressRepositoryImpl() {
        super(Address.class);
    }
}
