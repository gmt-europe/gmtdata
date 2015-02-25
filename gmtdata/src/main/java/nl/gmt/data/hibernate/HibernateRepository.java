package nl.gmt.data.hibernate;

import nl.gmt.data.DbContext;
import nl.gmt.data.DbQuery;
import nl.gmt.data.Entity;
import nl.gmt.data.Repository;
import org.apache.commons.lang.Validate;
import org.hibernate.Criteria;
import org.hibernate.LockOptions;
import org.hibernate.criterion.Projections;

import java.io.Serializable;
import java.util.List;

public abstract class HibernateRepository<T extends Entity> implements Repository<T> {
    private final Class<T> persistentClass;
    private DbContext context;

    protected HibernateRepository(Class<T> persistentClass) {
        this.persistentClass = persistentClass;
    }

    @Override
    public Class<T> getPersistentClass() {
        return persistentClass;
    }

    @Override
    public DbContext getContext() {
        return context;
    }

    @Override
    public void setContext(DbContext context) {
        this.context = context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> getAll() {
        return (List<T>)context.createCriteria(persistentClass).list();
    }

    @Override
    public int getCount() {
        Number count = (Number)context
            .createCriteria(persistentClass)
            .setProjection(Projections.rowCount())
            .uniqueResult();

        return count.intValue();
    }

    @Override
    public T get(Serializable id) {
        T result = find(id);

        Validate.isTrue(result != null, "Record not found");

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T find(Serializable id) {
        Validate.notNull(id, "id");

        return (T)get(persistentClass, id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T load(Serializable id) {
        return (T)load(persistentClass, id);
    }

    protected <R extends Repository> R getRepository(Class<R> repositoryClass) {
        return context.getRepository(repositoryClass);
    }

    protected Entity load(String entityName, Serializable id) {
        return context.load(entityName, id);
    }

    protected Entity get(String entityName, Serializable id) {
        return context.get(entityName, id);
    }

    protected void load(Entity entity, Serializable id) {
        context.load(entity, id);
    }

    protected Criteria createCriteria(String entityName, String alias) {
        return context.createCriteria(entityName, alias);
    }

    protected <E extends Entity> E load(Class<E> theClass, Serializable id, LockOptions lockOptions) {
        return context.load(theClass, id, lockOptions);
    }

    protected <TResult> DbQuery<TResult> createQuery(String queryString) {
        return context.createQuery(queryString);
    }

    protected Entity load(String entityName, Serializable id, LockOptions lockOptions) {
        return context.load(entityName, id, lockOptions);
    }

    protected Criteria createCriteria(Class persistentClass) {
        return context.createCriteria(persistentClass);
    }

    protected <E extends Entity> E get(Class<E> clazz, Serializable id, LockOptions lockOptions) {
        return context.get(clazz, id, lockOptions);
    }

    protected Criteria createCriteria(String entityName) {
        return context.createCriteria(entityName);
    }

    protected <E extends Entity> E get(Class<E> clazz, Serializable id) {
        return context.get(clazz, id);
    }

    protected Entity get(String entityName, Serializable id, LockOptions lockOptions) {
        return context.get(entityName, id, lockOptions);
    }

    protected Criteria createCriteria(Class persistentClass, String alias) {
        return context.createCriteria(persistentClass, alias);
    }

    protected <E extends Entity> E load(Class<E> theClass, Serializable id) {
        return context.load(theClass, id);
    }
}
