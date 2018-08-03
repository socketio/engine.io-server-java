package io.socket.engineio.server;

import io.socket.emitter.Emitter;
import io.socket.engineio.parser.Packet;
import io.socket.engineio.parser.Parser;
import io.socket.engineio.parser.ServerParser;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class TransportTest {

    private static final class TestTransport extends Transport {

        TestTransport() {
            super();
        }

        /* Abstract overrides */

        @Override
        public String getName() {
            return null;
        }

        @Override
        public boolean isWritable() {
            return false;
        }

        @Override
        public void send(List<Packet> packets) {
        }

        @Override
        protected void doClose() {
        }

        @Override
        public void onRequest(HttpServletRequest request, HttpServletResponse response) {
        }

        /* Test overrides */

        @Override
        public void onError(String reason, String description) {
            super.onError(reason, description);
        }

        @Override
        public void onPacket(Packet packet) {
            super.onPacket(packet);
        }

        @Override
        public void onData(Object data) {
            super.onData(data);
        }

        @Override
        public void onClose() {
            super.onClose();
        }

        /* Test methods */

        ReadyState getState() {
            return mReadyState;
        }

        @SuppressWarnings("SameParameterValue")
        void setState(ReadyState state) {
            mReadyState = state;
        }
    }

    @Test
    public void testInitialState() {
        final TestTransport transport = new TestTransport();
        assertEquals(ReadyState.OPEN, transport.getState());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testClose_open() {
        final TestTransport transport = Mockito.spy(new TestTransport());

        transport.close();
        Mockito.verify(transport, Mockito.times(1)).doClose();
        assertEquals(ReadyState.CLOSING, transport.getState());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testClose_closed() {
        final TestTransport transport = Mockito.spy(new TestTransport());

        transport.setState(ReadyState.CLOSED);
        transport.close();
        Mockito.verify(transport, Mockito.times(0)).doClose();
        assertEquals(ReadyState.CLOSED, transport.getState());
    }

    @Test
    public void testOnError() {
        final TestTransport transport = Mockito.spy(new TestTransport());

        transport.on("error", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
            }
        });
        transport.onError("test", null);
        Mockito.verify(transport, Mockito.times(1)).emit("error", "test", null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnPacket() {
        final TestTransport transport = Mockito.spy(new TestTransport());
        final Packet testPacket = new Packet(Packet.MESSAGE, "test");

        transport.onPacket(testPacket);
        Mockito.verify(transport, Mockito.times(1)).emit("packet", testPacket);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOnData() {
        final TestTransport transport = Mockito.spy(new TestTransport());

        final Packet testPacket = new Packet(Packet.MESSAGE, "test");
        ServerParser.encodePacket(testPacket, false, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                String packetData = (String) data;

                transport.onData(packetData);
                Mockito.verify(transport, Mockito.times(1)).onPacket(Mockito.any(Packet.class));
            }
        });
    }

    @Test
    public void testOnClose() {
        final TestTransport transport = Mockito.spy(new TestTransport());

        transport.onClose();
        assertEquals(transport.getState(), ReadyState.CLOSED);
        Mockito.verify(transport, Mockito.times(1)).emit("close");
    }
}