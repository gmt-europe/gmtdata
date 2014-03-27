package nl.gmt.data;

public interface DbContextListener {
    void beforeOpenContext(DbContext context);
    void afterOpenContext(DbContext context);
    void beforeCloseContext(DbContext context);
    void afterCloseContext(DbContext context);
}
