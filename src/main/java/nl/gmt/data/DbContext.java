package nl.gmt.data;

import org.hibernate.*;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.stat.SessionStatistics;
import org.jboss.logging.Logger;

import java.io.Serializable;
import java.sql.Connection;
import java.util.List;

public class DbContext implements AutoCloseable {
    private static final Logger log = Logger.getLogger(DbContext.class);

    private Transaction tx;
    private Session session;
    private DbConnection db;
    private DbContextState state;
    private List<DbContextListener> contextListeners;
    private boolean closed;

    DbContext(DbConnection db) {
        this.db = db;
        contextListeners = db.getContextListeners();

        state = DbContextState.UNKNOWN;

        for (DbContextListener listener : contextListeners) {
            listener.beforeOpenContext(this);
        }

        session = db.getSessionFactory().openSession();
        tx = session.beginTransaction();

        for (DbContextListener listener : contextListeners) {
            listener.afterOpenContext(this);
        }
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
        state = DbContextState.COMMITTED;
    }

    public void abort() {
        state = DbContextState.ABORTED;
    }

    @Override
    public void close() throws Exception {
        if (!closed) {
            if (session != null) {
                try {
                    if (state == DbContextState.UNKNOWN) {
                        log.warn("Aborting transaction because it was not committed or rolled back");
                        state = DbContextState.ABORTED;
                    }

                    for (DbContextListener listener : contextListeners) {
                        listener.beforeCloseContext(this);
                    }

                    if (state == DbContextState.COMMITTED) {
                        session.flush();
                        tx.commit();
                    } else {
                        tx.rollback();
                    }

                    session.close();
                    session = null;
                } finally {
                    for (DbContextListener listener : contextListeners) {
                        listener.afterCloseContext(this);
                    }
                }
            }

            closed = true;
        }
    }

    public void refresh(Object object, LockOptions lockOptions) {
        session.refresh(object, lockOptions);
    }

    public boolean isDirty() throws HibernateException {
        return session.isDirty();
    }

    public void evict(Object object) {
        session.evict(object);
    }

    public Object load(String entityName, Serializable id) {
        return session.load(entityName, id);
    }

    public void refresh(Object object) {
        session.refresh(object);
    }

    public Object get(String entityName, Serializable id) {
        return session.get(entityName, id);
    }

    public Serializable save(String entityName, Object object) {
        return session.save(entityName, object);
    }

    public void load(Object object, Serializable id) {
        session.load(object, id);
    }

    public Criteria createCriteria(String entityName, String alias) {
        return session.createCriteria(entityName, alias);
    }

    public void persist(Object object) {
        session.persist(object);
    }

    public Object load(Class theClass, Serializable id, LockOptions lockOptions) {
        return session.load(theClass, id, lockOptions);
    }

    public void saveOrUpdate(Object object) {
        session.saveOrUpdate(object);
    }

    public void saveOrUpdate(String entityName, Object object) {
        session.saveOrUpdate(entityName, object);
    }

    public void update(String entityName, Object object) {
        session.update(entityName, object);
    }

    public void persist(String entityName, Object object) {
        session.persist(entityName, object);
    }

    public Object merge(String entityName, Object object) {
        return session.merge(entityName, object);
    }

    public void refresh(String entityName, Object object) {
        session.refresh(entityName, object);
    }

    public boolean contains(Object object) {
        return session.contains(object);
    }

    public Serializable save(Object object) {
        return session.save(object);
    }

    public <T> DbQuery<T> createQuery(String queryString) {
        return new DbQuery<T>(session.createQuery(queryString));
    }

    public Object load(String entityName, Serializable id, LockOptions lockOptions) {
        return session.load(entityName, id, lockOptions);
    }

    public void replicate(Object object, ReplicationMode replicationMode) {
        session.replicate(object, replicationMode);
    }

    public Object merge(Object object) {
        return session.merge(object);
    }

    public Criteria createCriteria(Class persistentClass) {
        return session.createCriteria(persistentClass);
    }

    public void replicate(String entityName, Object object, ReplicationMode replicationMode) {
        session.replicate(entityName, object, replicationMode);
    }

    public void delete(Object object) {
        session.delete(object);
    }

    public void update(Object object) {
        session.update(object);
    }

    public Object get(Class clazz, Serializable id, LockOptions lockOptions) {
        return session.get(clazz, id, lockOptions);
    }

    public Criteria createCriteria(String entityName) {
        return session.createCriteria(entityName);
    }

    public String getEntityName(Object object) {
        return session.getEntityName(object);
    }

    public void delete(String entityName, Object object) {
        session.delete(entityName, object);
    }

    public Object get(Class clazz, Serializable id) {
        return session.get(clazz, id);
    }

    public void clear() {
        session.clear();
    }

    public Object get(String entityName, Serializable id, LockOptions lockOptions) {
        return session.get(entityName, id, lockOptions);
    }

    public void refresh(String entityName, Object object, LockOptions lockOptions) {
        session.refresh(entityName, object, lockOptions);
    }

    public Criteria createCriteria(Class persistentClass, String alias) {
        return session.createCriteria(persistentClass, alias);
    }

    public boolean isReadOnly(Object entityOrProxy) {
        return session.isReadOnly(entityOrProxy);
    }

    public Object load(Class theClass, Serializable id) {
        return session.load(theClass, id);
    }

    public void setReadOnly(Object entityOrProxy, boolean readOnly) {
        session.setReadOnly(entityOrProxy, readOnly);
    }
}
