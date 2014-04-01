package nl.gmt.data;

import java.io.Serializable;
import java.util.List;

public interface Repository<T extends Entity> {
    DbContext getContext();
    void setContext(DbContext context);
    List<T> getAll();
    int getCount();
    T get(Serializable id);
    T find(Serializable id);
    T load(Serializable id);
}
