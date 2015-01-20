package nl.gmt.data.module1.model.implementation;

import nl.gmt.data.hibernate.HibernateRepository;

import nl.gmt.data.module1.model.Relation;
import nl.gmt.data.module1.model.RelationRepository;

@SuppressWarnings("UnusedDeclaration")
public class RelationRepositoryImpl extends HibernateRepository<Relation> implements RelationRepository {
    public RelationRepositoryImpl() {
        super(Relation.class);
    }
}
