package nl.gmt.data.baseModule.model.implementation;

import nl.gmt.data.hibernate.HibernateRepository;

import nl.gmt.data.baseModule.model.User;
import nl.gmt.data.baseModule.model.UserRepository;

@SuppressWarnings("UnusedDeclaration")
public class UserRepositoryImpl extends HibernateRepository<User> implements UserRepository {
    public UserRepositoryImpl() {
        super(User.class);
    }
}
