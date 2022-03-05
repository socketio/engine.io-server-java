package io.socket.engineio.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * The event emitter which is ported from the JavaScript module. This class is thread-safe.
 *
 * @see <a href="https://github.com/component/emitter">https://github.com/component/emitter</a>
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public class Emitter {

    public interface Listener {

        void call(Object... args);
    }

    private class OnceListener implements Listener {

        public final String event;
        public final Listener fn;

        public OnceListener(String event, Listener fn) {
            this.event = event;
            this.fn = fn;
        }

        @Override
        public void call(Object... args) {
            Emitter.this.off(this.event, this);
            this.fn.call(args);
        }
    }

    private final ConcurrentMap<String, ConcurrentLinkedQueue<Listener>> mCallbacks = new ConcurrentHashMap<>();

    /**
     * Listens on the event.
     * @param event event name.
     * @param fn Event listener.
     * @return a reference to this object.
     */
    public Emitter on(String event, Listener fn) {
        this.mCallbacks.computeIfAbsent(event, s -> new ConcurrentLinkedQueue<>());
        final ConcurrentLinkedQueue<Listener> callbacks = this.mCallbacks.get(event);
        callbacks.add(fn);
        return this;
    }

    /**
     * Adds a one time listener for the event.
     *
     * @param event an event name.
     * @param fn Event listener.
     * @return a reference to this object.
     */
    public Emitter once(final String event, final Listener fn) {
        this.on(event, new OnceListener(event, fn));
        return this;
    }

    /**
     * Removes all registered listeners.
     *
     * @return a reference to this object.
     */
    public Emitter off() {
        this.mCallbacks.clear();
        return this;
    }

    /**
     * Removes all listeners of the specified event.
     *
     * @param event an event name.
     * @return a reference to this object.
     */
    public Emitter off(String event) {
        this.mCallbacks.remove(event);
        return this;
    }

    /**
     * Removes the listener.
     *
     * @param event an event name.
     * @param fn Event listener.
     * @return a reference to this object.
     */
    public Emitter off(String event, Listener fn) {
        final ConcurrentLinkedQueue<Listener> callbacks = this.mCallbacks.get(event);
        if (callbacks != null) {
            Iterator<Listener> it = callbacks.iterator();
            while (it.hasNext()) {
                Listener internal = it.next();
                if (Emitter.sameAs(fn, internal)) {
                    it.remove();
                    break;
                }
            }
        }
        return this;
    }

    /**
     * Executes each of listeners with the given args.
     *
     * @param event an event name.
     * @param args Data to emit.
     * @return a reference to this object.
     */
    public Emitter emit(String event, Object... args) {
        final ConcurrentLinkedQueue<Listener> callbacks = this.mCallbacks.get(event);
        if (callbacks != null) {
            for (Listener fn : callbacks) {
                try {
                    fn.call(args);
                } catch (Exception ignore) {
                }
            }
        }
        return this;
    }

    /**
     * Returns a list of listeners for the specified event.
     * The returned list is not modifiable.
     *
     * @param event an event name.
     * @return a reference to this object.
     */
    public List<Listener> listeners(String event) {
        final ConcurrentLinkedQueue<Listener> callbacks = this.mCallbacks.get(event);
        return callbacks != null ? Collections.unmodifiableList(new ArrayList<>(callbacks)) : Collections.emptyList();
    }

    /**
     * Check if this emitter has listeners for the specified event.
     *
     * @param event an event name.
     * @return a reference to this object.
     */
    public boolean hasListeners(String event) {
        final ConcurrentLinkedQueue<Listener> callbacks = this.mCallbacks.get(event);
        return callbacks != null && !callbacks.isEmpty();
    }

    private static boolean sameAs(Listener fn, Listener internal) {
        if (fn.equals(internal)) {
            return true;
        } else if (internal instanceof OnceListener) {
            return fn.equals(((OnceListener) internal).fn);
        } else {
            return false;
        }
    }
}
