package nl.gmt.data.superModule.model.implementation;

import nl.gmt.data.hibernate.HibernateRepository;

import nl.gmt.data.superModule.model.RelationAddress;
import nl.gmt.data.superModule.model.RelationAddressRepository;

@SuppressWarnings("UnusedDeclaration")
public class RelationAddressRepositoryImpl extends HibernateRepository<RelationAddress> implements RelationAddressRepository {
    public RelationAddressRepositoryImpl() {
        super(RelationAddress.class);
    }
}
