package nl.gmt.data.test.model;

import nl.gmt.data.Repository;

public interface RelationRepository extends Repository<Relation> {
    Relation findByName(String name);
}
