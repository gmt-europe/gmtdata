package nl.gmt.data.model;

import nl.gmt.data.Repository;

public interface RelationRepository extends Repository<Relation> {
    Relation findByName(String name);
}
