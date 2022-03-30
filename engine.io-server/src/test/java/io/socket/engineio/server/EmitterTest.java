package io.socket.engineio.server;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public final class EmitterTest {

    @Test
    public void testSingleListener() {
        final Emitter emitter = new Emitter();
        final Emitter.Listener listener = Mockito.mock(Emitter.Listener.class);

        emitter.on("event", listener);
        emitter.emit("event", "Engine.IO");

        Assert.assertTrue(emitter.hasListeners("event"));

        Mockito.verify(listener, Mockito.times(1))
                .call(Mockito.eq("Engine.IO"));
    }

    @Test
    public void testMultipleListenersSameEvent() {
        final Emitter emitter = new Emitter();
        final Emitter.Listener[] listeners = new Emitter.Listener[3];
        for (int i = 0; i < listeners.length; i++) {
            listeners[i] = Mockito.mock(Emitter.Listener.class);
            emitter.on("event", listeners[i]);
        }

        emitter.emit("event", "Engine.IO");

        for (Emitter.Listener listener : listeners) {
            Mockito.verify(listener, Mockito.times(1))
                    .call(Mockito.eq("Engine.IO"));
        }
    }

    @Test
    public void testMultipleListenersDifferentEvents() {
        final Emitter emitter = new Emitter();
        final Emitter.Listener[] listeners = new Emitter.Listener[4];
        for (int i = 0; i < listeners.length; i++) {
            listeners[i] = Mockito.mock(Emitter.Listener.class);
        }

        emitter.on("event0", listeners[0]);
        emitter.on("event1", listeners[0]);
        emitter.on("event2", listeners[0]);
        emitter.on("event1", listeners[1]);
        emitter.on("event2", listeners[2]);

        emitter.emit("event0");
        emitter.emit("event1");
        emitter.emit("event2");
        emitter.emit("event3");

        Mockito.verify(listeners[0], Mockito.times(3)).call();
        Mockito.verify(listeners[1], Mockito.times(1)).call();
        Mockito.verify(listeners[2], Mockito.times(1)).call();
        Mockito.verify(listeners[3], Mockito.times(0)).call();
    }

    @Test
    public void testOnceListener() {
        final Emitter emitter = new Emitter();
        final Emitter.Listener listener1 = Mockito.mock(Emitter.Listener.class);
        final Emitter.Listener listener2 = Mockito.mock(Emitter.Listener.class);

        emitter.once("event", listener1);
        emitter.on("event", listener2);

        emitter.emit("event");
        emitter.emit("event");

        Mockito.verify(listener1, Mockito.times(1)).call();
        Mockito.verify(listener2, Mockito.times(2)).call();
    }

    @Test
    public void testAllOff() {
        final Emitter emitter = new Emitter();
        final Emitter.Listener listener = Mockito.mock(Emitter.Listener.class);

        emitter.on("event0", listener);
        emitter.on("event1", listener);

        emitter.emit("event0");
        emitter.emit("event1");

        Mockito.verify(listener, Mockito.times(2)).call();

        emitter.off();
        emitter.emit("event0");
        emitter.emit("event1");

        Mockito.verify(listener, Mockito.times(2)).call();
    }

    @Test
    public void testEventOff() {
        final Emitter emitter = new Emitter();
        final Emitter.Listener listener = Mockito.mock(Emitter.Listener.class);

        emitter.on("event0", listener);
        emitter.on("event1", listener);

        Assert.assertTrue(emitter.hasListeners("event0"));
        Assert.assertTrue(emitter.hasListeners("event1"));

        emitter.emit("event0");
        emitter.emit("event1");

        Mockito.verify(listener, Mockito.times(2)).call();

        emitter.off("event0");

        Assert.assertFalse(emitter.hasListeners("event0"));
        Assert.assertTrue(emitter.hasListeners("event1"));

        emitter.emit("event0");
        emitter.emit("event1");

        Mockito.verify(listener, Mockito.times(3)).call();
    }

    @Test
    public void testListenerOff() {
        final Emitter emitter = new Emitter();
        final Emitter.Listener listener0 = Mockito.mock(Emitter.Listener.class);
        final Emitter.Listener listener1 = Mockito.mock(Emitter.Listener.class);

        emitter.on("event0", listener0);
        emitter.on("event1", listener0);
        emitter.on("event0", listener1);
        emitter.on("event1", listener1);

        emitter.emit("event0");
        emitter.emit("event1");

        Mockito.verify(listener0, Mockito.times(2)).call();
        Mockito.verify(listener1, Mockito.times(2)).call();

        emitter.off("event0", listener1);

        emitter.emit("event0");
        emitter.emit("event1");

        Mockito.verify(listener0, Mockito.times(4)).call();
        Mockito.verify(listener1, Mockito.times(3)).call();
    }

    @Test
    public void testListenerException() {
        final Emitter emitter = new Emitter();

        final Emitter.Listener listener0 = Mockito.mock(Emitter.Listener.class);
        Mockito.doThrow(new RuntimeException("Here")).when(listener0).call();

        final Emitter.Listener listener1 = Mockito.mock(Emitter.Listener.class);

        emitter.on("event0", listener0);
        emitter.on("event1", listener0);
        emitter.on("event0", listener1);
        emitter.on("event1", listener1);

        emitter.emit("event0");
        emitter.emit("event1");

        Mockito.verify(listener0, Mockito.times(2)).call();
        Mockito.verify(listener1, Mockito.times(2)).call();
    }
}
