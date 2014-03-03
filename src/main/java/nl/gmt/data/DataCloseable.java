package nl.gmt.data;

public interface DataCloseable extends AutoCloseable {
    @Override
    void close();
}
