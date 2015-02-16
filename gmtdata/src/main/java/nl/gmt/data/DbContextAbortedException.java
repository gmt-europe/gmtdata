package nl.gmt.data;

public class DbContextAbortedException extends RuntimeException {
    public DbContextAbortedException(String message) {
        super(message);
    }
}
