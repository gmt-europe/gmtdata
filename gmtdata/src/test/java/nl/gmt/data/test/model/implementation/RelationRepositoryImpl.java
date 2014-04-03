package nl.gmt.data.test.model.implementation;

import nl.gmt.data.hibernate.HibernateRepository;
import nl.gmt.data.test.model.Relation;
import nl.gmt.data.test.model.RelationRepository;

public class RelationRepositoryImpl extends HibernateRepository<Relation> implements RelationRepository {
    public RelationRepositoryImpl() {
        super(Relation.class);
    }

    @Override
    public Relation findByName(String name) {
        return this.<Relation>createQuery("from Relation r where r.name = :name")
            .setString("name", name)
            .uniqueResult();
    }
}
