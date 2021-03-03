package org.thesis;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class ListenerThread extends Thread {

    public interface ThreadCompleteListener {

        void notifyOfThreadComplete(final Thread thread);
    }

    private final Set<ThreadCompleteListener> listeners
            = new CopyOnWriteArraySet<ThreadCompleteListener>();
    public final void addListener(final ThreadCompleteListener listener) {
        if (listener != null)
            listeners.add(listener);
    }
    public final void removeListener(final ThreadCompleteListener listener) {
        listeners.remove(listener);
    }
    protected final void notifyListeners() {
        for (ThreadCompleteListener listener : listeners) {
            if (listener != null)
                listener.notifyOfThreadComplete(this);
        }
    }
}
