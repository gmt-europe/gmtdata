package nl.gmt.data;

import nl.gmt.data.support.DelegateListener;
import nl.gmt.data.support.Delegate;
import org.apache.commons.lang.Validate;
import org.hibernate.*;
import org.jboss.logging.Logger;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DbContext implements DataCloseable {
    private static final Logger LOG = Logger.getLogger(DbContext.class);
    private static boolean throwExceptionOnAbort;

    public static void setThrowExceptionOnAbort(boolean value) {
        throwExceptionOnAbort = value;
    }

    private final Transaction tx;
    private final DbConnection db;
    private final DbTenant tenant;
    private Session session;
    private DbContextState state;
    private final Delegate<DbContextTransition> transitioned = new Delegate<>();
    private boolean closed;
    private Map<String, Object> userProperties;

    DbContext(DbConnection db, DbTenant tenant) {
        Validate.notNull(db, "db");

        this.db = db;
        this.tenant = tenant;

        state = DbContextState.UNKNOWN;

        raiseTransitioned(DbContextTransition.OPENING);

        try {
            if (tenant != null) {
                session = db.getSessionFactory().withOptions().tenantIdentifier(tenant.getDatabase()).openSession();
            } else {
                session = db.getSessionFactory().openSession();
            }

            tx = session.beginTransaction();

            raiseTransitioned(DbContextTransition.OPENED);

            db.setCurrentContext(this);
        } catch (Throwable e) {
            // Ensure that the context is properly cleaned up if we failed to open
            // the context.

            try {
                close();
            } catch (Exception e1) {
                e = new RuntimeException(e1);
            }

            if (!(e instanceof RuntimeException)) {
                e = new RuntimeException(e);
            }

            throw (RuntimeException)e;
        }
    }

    public DbConnection getConnection() {
        return db;
    }

    public DbTenant getTenant() {
        return tenant;
    }

    private void raiseTransitioned(DbContextTransition transition) {
        db.raiseContextTransitioned(this, transition);

        transitioned.call(this, transition);
    }

    public void addTransitioned(DelegateListener<DbContextTransition> listener) {
        transitioned.add(listener);
    }

    public boolean removeTransitioned(DelegateListener<DbContextTransition> listener) {
        return transitioned.remove(listener);
    }

    public Session getSession() {
        return session;
    }

    public boolean isClosed() {
        return closed;
    }

    public DbContextState getState() {
        return state;
    }

    public void commit() {
        // Don't commit the session if it was aborted already. The reason for this is e.g. the Struts implementation.
        // The DbInterceptor will always commit a transaction. If you need to abort the transaction, you can call abort
        // but this method will overwrite the state to COMMITTED. Only setting the state when it currently
        // is UNKNOWN allows you to still abort the transaction.

        if (state == DbContextState.UNKNOWN) {
            state = DbContextState.COMMITTED;
        }
    }

    public void abort() {
        state = DbContextState.ABORTED;
    }

    public <T extends Repository> T getRepository(Class<T> repositoryClass) {
        RepositoryService repositoryService = db.getRepositoryService();

        if (repositoryService == null) {
            throw new IllegalStateException("Repository service is not available");
        }

        T repository = repositoryService.getRepository(repositoryClass);

        repository.setContext(this);

        return repository;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        db.clearCurrentContext();

        boolean aborted = false;

        try {
            if (session != null) {
                try {
                    if (state == DbContextState.UNKNOWN) {
                        LOG.warn("Aborting transaction because it was not committed or rolled back");
                        state = DbContextState.ABORTED;
                        aborted = true;
                    }

                    raiseTransitioned(DbContextTransition.CLOSING);

                    boolean success = false;

                    try {
                        if (state == DbContextState.COMMITTED) {
                            session.flush();
                            tx.commit();
                            success = true;
                        }
                    } finally {
                        if (!success) {
                            tx.rollback();
                        }
                    }
                } finally {
                    session.close();
                    session = null;
                }
            }
        } finally {
            raiseTransitioned(DbContextTransition.CLOSED);
        }

        closed = true;

        if (aborted && throwExceptionOnAbort) {
            throw new DbContextAbortedException("Aborting transaction because it was not committed or rolled back");
        }
    }

    public void refresh(Entity entity, LockOptions lockOptions) {
        session.refresh(entity, lockOptions);
    }

    public boolean isDirty() throws HibernateException {
        return session.isDirty();
    }

    public void evict(Entity entity) {
        session.evict(entity);
    }

    public Entity load(String entityName, Serializable id) {
        return (Entity)session.load(entityName, id);
    }

    public void refresh(Entity entity) {
        session.refresh(entity);
    }

    public Entity get(String entityName, Serializable id) {
        return (Entity)session.get(entityName, id);
    }

    public Serializable save(String entityName, Entity entity) {
        return session.save(entityName, entity);
    }

    public void load(Entity entity, Serializable id) {
        session.load(entity, id);
    }

    public Criteria createCriteria(String entityName, String alias) {
        return session.createCriteria(entityName, alias);
    }

    public void persist(Entity entity) {
        session.persist(entity);
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> T load(Class<T> theClass, Serializable id, LockOptions lockOptions) {
        return (T)session.load(theClass, id, lockOptions);
    }

    public void saveOrUpdate(Entity entity) {
        session.saveOrUpdate(entity);
    }

    public void saveOrUpdate(String entityName, Entity entity) {
        session.saveOrUpdate(entityName, entity);
    }

    public void update(String entityName, Entity entity) {
        session.update(entityName, entity);
    }

    public void persist(String entityName, Entity entity) {
        session.persist(entityName, entity);
    }

    public Entity merge(String entityName, Entity entity) {
        return (Entity)session.merge(entityName, entity);
    }

    public void refresh(String entityName, Entity entity) {
        session.refresh(entityName, entity);
    }

    public boolean contains(Entity entity) {
        return session.contains(entity);
    }

    public Serializable save(Entity entity) {
        return session.save(entity);
    }

    public <T> DbQuery<T> createQuery(String queryString) {
        return new DbQuery<T>(session.createQuery(queryString));
    }

    public Entity load(String entityName, Serializable id, LockOptions lockOptions) {
        return (Entity)session.load(entityName, id, lockOptions);
    }

    public void replicate(Entity entity, ReplicationMode replicationMode) {
        session.replicate(entity, replicationMode);
    }

    public Entity merge(Entity entity) {
        return (Entity)session.merge(entity);
    }

    public Criteria createCriteria(Class persistentClass) {
        return session.createCriteria(persistentClass);
    }

    public void replicate(String entityName, Entity entity, ReplicationMode replicationMode) {
        session.replicate(entityName, entity, replicationMode);
    }

    public void delete(Entity entity) {
        session.delete(entity);
    }

    public void update(Entity entity) {
        session.update(entity);
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> T get(Class<T> clazz, Serializable id, LockOptions lockOptions) {
        return (T)session.get(clazz, id, lockOptions);
    }

    public Criteria createCriteria(String entityName) {
        return session.createCriteria(entityName);
    }

    public String getEntityName(Entity entity) {
        return session.getEntityName(entity);
    }

    public void delete(String entityName, Entity entity) {
        session.delete(entityName, entity);
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> T get(Class<T> clazz, Serializable id) {
        return (T)session.get(clazz, id);
    }

    public void clear() {
        session.clear();
    }

    public Entity get(String entityName, Serializable id, LockOptions lockOptions) {
        return (Entity)session.get(entityName, id, lockOptions);
    }

    public void refresh(String entityName, Entity entity, LockOptions lockOptions) {
        session.refresh(entityName, entity, lockOptions);
    }

    public Criteria createCriteria(Class persistentClass, String alias) {
        return session.createCriteria(persistentClass, alias);
    }

    public boolean isReadOnly(Entity entityOrProxy) {
        return session.isReadOnly(entityOrProxy);
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> T load(Class<T> theClass, Serializable id) {
        return (T)session.load(theClass, id);
    }

    public void setReadOnly(Entity entityOrProxy, boolean readOnly) {
        session.setReadOnly(entityOrProxy, readOnly);
    }

    public Map<String, Object> getUserProperties() {
        if (userProperties == null) {
            userProperties = new HashMap<>();
        }

        return userProperties;
    }
}
