package nl.gmt.data.support;

import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class Delegate<T> {
    private final AtomicReference<DelegateListener[]> listeners = new AtomicReference<>(new DelegateListener[0]);

    public void add(DelegateListener<T> listener) {
        Validate.notNull(listener, "listener");

        while (true) {
            DelegateListener[] listeners = this.listeners.get();

            DelegateListener[] newListeners = Arrays.copyOf(listeners, listeners.length + 1);
            newListeners[newListeners.length - 1] = listener;

            if (this.listeners.compareAndSet(listeners, newListeners)) {
                return;
            }
        }
    }

    public boolean remove(DelegateListener<T> listener) {
        Validate.notNull(listener, "listener");

        while (true) {
            DelegateListener[] listeners = this.listeners.get();

            int pos = -1;

            for (int i = 0; i < listeners.length; i++) {
                if (listeners[i] == listener) {
                    pos = i;
                    break;
                }
            }

            if (pos == -1) {
                return false;
            }

            DelegateListener[] newListeners = new DelegateListener[listeners.length - 1];

            int offset = 0;
            for (int i = 0; i < listeners.length; i++) {
                if (i == pos) {
                    continue;
                }

                newListeners[offset++] = listeners[i];
            }

            if (this.listeners.compareAndSet(listeners, newListeners)) {
                return true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void call(Object sender, T arg) {
        DelegateListener[] listeners = this.listeners.get();

        for (DelegateListener<T> listener : listeners) {
            listener.call(sender, arg);
        }
    }

    public boolean isEmpty() {
        return this.listeners.get().length == 0;
    }
}
