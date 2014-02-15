package nl.gmt.data;

import org.hibernate.*;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class DbQuery<T> implements Iterable<T> {
    private Query query;

    DbQuery(Query query) {
        this.query = query;
    }

    public String getQueryString() {
        return query.getQueryString();
    }

    public DbQuery<T> setLockOptions(LockOptions lockOptions) {
        query.setLockOptions(lockOptions);
        return this;
    }

    public DbQuery<T> setParameter(String name, Object val) {
        query.setParameter(name, val);
        return this;
    }

    public DbQuery<T> setFetchSize(int fetchSize) {
        query.setFetchSize(fetchSize);
        return this;
    }

    public DbQuery<T> setParameter(int position, Object val) {
        query.setParameter(position, val);
        return this;
    }

    public DbQuery<T> setBoolean(int position, boolean val) {
        query.setBoolean(position, val);
        return this;
    }

    public DbQuery<T> setEntity(int position, Object val) {
        query.setEntity(position, val);
        return this;
    }

    public DbQuery<T> setReadOnly(boolean readOnly) {
        query.setReadOnly(readOnly);
        return this;
    }

    public DbQuery<T> setFloat(String name, float val) {
        query.setFloat(name, val);
        return this;
    }

    public DbQuery<T> setParameter(String name, Object val, Type type) {
        query.setParameter(name, val, type);
        return this;
    }

    public DbQuery<T> setBinary(int position, byte[] val) {
        query.setBinary(position, val);
        return this;
    }

    public DbQuery<T> setCacheRegion(String cacheRegion) {
        query.setCacheRegion(cacheRegion);
        return this;
    }

    public DbQuery<T> setSerializable(String name, Serializable val) {
        query.setSerializable(name, val);
        return this;
    }

    public DbQuery<T> setText(int position, String val) {
        query.setText(position, val);
        return this;
    }

    public DbQuery<T> setTimestamp(int position, Date date) {
        query.setTimestamp(position, date);
        return this;
    }

    public DbQuery<T> setComment(String comment) {
        query.setComment(comment);
        return this;
    }

    public DbQuery<T> setEntity(String name, Object val) {
        query.setEntity(name, val);
        return this;
    }

    public DbQuery<T> setFirstResult(int firstResult) {
        query.setFirstResult(firstResult);
        return this;
    }

    public ScrollableResults scroll(ScrollMode scrollMode) {
        return query.scroll(scrollMode);
    }

    public DbQuery<T> setCharacter(String name, char val) {
        query.setCharacter(name, val);
        return this;
    }

    public String[] getNamedParameters() {
        return query.getNamedParameters();
    }

    public List<T> list() {
        return (List<T>)query.list();
    }

    public DbQuery<T> setCalendarDate(String name, Calendar calendar) {
        query.setCalendarDate(name, calendar);
        return this;
    }

    public DbQuery<T> setCalendar(String name, Calendar calendar) {
        query.setCalendar(name, calendar);
        return this;
    }

    public DbQuery<T> setCharacter(int position, char val) {
        query.setCharacter(position, val);
        return this;
    }

    public DbQuery<T> setLong(String name, long val) {
        query.setLong(name, val);
        return this;
    }

    public FlushMode getFlushMode() {
        return query.getFlushMode();
    }

    public DbQuery<T> setParameterList(String name, Collection values) {
        query.setParameterList(name, values);
        return this;
    }

    public DbQuery<T> addQueryHint(String hint) {
        query.addQueryHint(hint);
        return this;
    }

    public DbQuery<T> setTime(String name, Date date) {
        query.setTime(name, date);
        return this;
    }

    public String getComment() {
        return query.getComment();
    }

    public DbQuery<T> setTime(int position, Date date) {
        query.setTime(position, date);
        return this;
    }

    public String getCacheRegion() {
        return query.getCacheRegion();
    }

    public DbQuery<T> setBigDecimal(int position, BigDecimal number) {
        query.setBigDecimal(position, number);
        return this;
    }

    public DbQuery<T> setText(String name, String val) {
        query.setText(name, val);
        return this;
    }

    public DbQuery<T> setBigInteger(int position, BigInteger number) {
        query.setBigInteger(position, number);
        return this;
    }

    public Integer getMaxResults() {
        return query.getMaxResults();
    }

    public DbQuery<T> setDate(String name, Date date) {
        query.setDate(name, date);
        return this;
    }

    public String[] getReturnAliases() {
        return query.getReturnAliases();
    }

    public DbQuery<T> setTimeout(int timeout) {
        query.setTimeout(timeout);
        return this;
    }

    public Object uniqueResult() {
        return query.uniqueResult();
    }

    public DbQuery<T> setProperties(Object bean) {
        query.setProperties(bean);
        return this;
    }

    public DbQuery<T> setShort(int position, short val) {
        query.setShort(position, val);
        return this;
    }

    public DbQuery<T> setByte(String name, byte val) {
        query.setByte(name, val);
        return this;
    }

    public DbQuery<T> setBoolean(String name, boolean val) {
        query.setBoolean(name, val);
        return this;
    }

    public boolean isReadOnly() {
        return query.isReadOnly();
    }

    public CacheMode getCacheMode() {
        return query.getCacheMode();
    }

    public Integer getTimeout() {
        return query.getTimeout();
    }

    public boolean isCacheable() {
        return query.isCacheable();
    }

    public DbQuery<T> setCalendar(int position, Calendar calendar) {
        query.setCalendar(position, calendar);
        return this;
    }

    public DbQuery<T> setCacheMode(CacheMode cacheMode) {
        query.setCacheMode(cacheMode);
        return this;
    }

    @Override
    public Iterator<T> iterator() {
        final Iterator iterator = query.iterate();

        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return (T)iterator.next();
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }

    public DbQuery<T> setBinary(String name, byte[] val) {
        query.setBinary(name, val);
        return this;
    }

    public DbQuery<T> setBigDecimal(String name, BigDecimal number) {
        query.setBigDecimal(name, number);
        return this;
    }

    public int executeUpdate() {
        return query.executeUpdate();
    }

    public DbQuery<T> setLong(int position, long val) {
        query.setLong(position, val);
        return this;
    }

    public DbQuery<T> setParameterList(String name, Object[] values) {
        query.setParameterList(name, values);
        return this;
    }

    public DbQuery<T> setResultTransformer(ResultTransformer transformer) {
        query.setResultTransformer(transformer);
        return this;
    }

    public DbQuery<T> setByte(int position, byte val) {
        query.setByte(position, val);
        return this;
    }

    public Integer getFetchSize() {
        return query.getFetchSize();
    }

    public DbQuery<T> setBigInteger(String name, BigInteger number) {
        query.setBigInteger(name, number);
        return this;
    }

    public DbQuery<T> setString(String name, String val) {
        query.setString(name, val);
        return this;
    }

    public LockOptions getLockOptions() {
        return query.getLockOptions();
    }

    public DbQuery<T> setMaxResults(int maxResults) {
        query.setMaxResults(maxResults);
        return this;
    }

    public DbQuery<T> setInteger(int position, int val) {
        query.setInteger(position, val);
        return this;
    }

    public DbQuery<T> setCalendarDate(int position, Calendar calendar) {
        query.setCalendarDate(position, calendar);
        return this;
    }

    public DbQuery<T> setTimestamp(String name, Date date) {
        query.setTimestamp(name, date);
        return this;
    }

    public DbQuery<T> setString(int position, String val) {
        query.setString(position, val);
        return this;
    }

    public DbQuery<T> setInteger(String name, int val) {
        query.setInteger(name, val);
        return this;
    }

    public DbQuery<T> setParameterList(String name, Object[] values, Type type) {
        query.setParameterList(name, values, type);
        return this;
    }

    public DbQuery<T> setParameter(int position, Object val, Type type) {
        query.setParameter(position, val, type);
        return this;
    }

    public DbQuery<T> setSerializable(int position, Serializable val) {
        query.setSerializable(position, val);
        return this;
    }

    public DbQuery<T> setShort(String name, short val) {
        query.setShort(name, val);
        return this;
    }

    public DbQuery<T> setFloat(int position, float val) {
        query.setFloat(position, val);
        return this;
    }

    public ScrollableResults scroll() {
        return query.scroll();
    }

    public DbQuery<T> setCacheable(boolean cacheable) {
        query.setCacheable(cacheable);
        return this;
    }

    public DbQuery<T> setLockMode(String alias, LockMode lockMode) {
        query.setLockMode(alias, lockMode);
        return this;
    }

    public Integer getFirstResult() {
        return query.getFirstResult();
    }

    public DbQuery<T> setLocale(int position, Locale locale) {
        query.setLocale(position, locale);
        return this;
    }

    public DbQuery<T> setFlushMode(FlushMode flushMode) {
        query.setFlushMode(flushMode);
        return this;
    }

    public DbQuery<T> setLocale(String name, Locale locale) {
        query.setLocale(name, locale);
        return this;
    }

    public DbQuery<T> setDouble(String name, double val) {
        query.setDouble(name, val);
        return this;
    }

    public DbQuery<T> setDouble(int position, double val) {
        query.setDouble(position, val);
        return this;
    }

    public DbQuery<T> setProperties(Map bean) {
        query.setProperties(bean);
        return this;
    }

    public Type[] getReturnTypes() {
        return query.getReturnTypes();
    }

    public DbQuery<T> setDate(int position, Date date) {
        query.setDate(position, date);
        return this;
    }

    public DbQuery<T> setParameterList(String name, Collection values, Type type) {
        query.setParameterList(name, values, type);
        return this;
    }

    public DbQuery<T> setParameters(Object[] values, Type[] types) {
        query.setParameters(values, types);
        return this;
    }
}
