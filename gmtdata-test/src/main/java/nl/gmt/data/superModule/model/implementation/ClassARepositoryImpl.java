package nl.gmt.data.superModule.model.implementation;

import nl.gmt.data.hibernate.HibernateRepository;

import nl.gmt.data.superModule.model.ClassA;
import nl.gmt.data.superModule.model.ClassARepository;

@SuppressWarnings("UnusedDeclaration")
public class ClassARepositoryImpl extends HibernateRepository<ClassA> implements ClassARepository {
    public ClassARepositoryImpl() {
        super(ClassA.class);
    }
}
