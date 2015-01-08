package nl.gmt.data.support;

public interface DelegateListener<T> {
    void call(Object sender, T arg);
}
