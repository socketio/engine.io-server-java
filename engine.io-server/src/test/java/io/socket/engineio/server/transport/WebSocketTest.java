package io.socket.engineio.server.transport;

import io.socket.emitter.Emitter;
import io.socket.engineio.parser.Packet;
import io.socket.engineio.parser.Parser;
import io.socket.engineio.server.EngineIoWebSocket;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class WebSocketTest {

    private static final class EngineIoWebSocketStub extends EngineIoWebSocket {

        @Override
        public Map<String, String> getQuery() {
            return new HashMap<>();
        }

        @Override
        public Map<String, List<String>> getConnectionHeaders() {
            return new HashMap<>();
        }

        @Override
        public void write(String message) {
        }

        @Override
        public void write(byte[] message) {
        }

        @Override
        public void close() {
        }
    }

    @Test
    public void testName() {
        final WebSocket webSocket = new WebSocket(Mockito.mock(EngineIoWebSocket.class), Parser.PROTOCOL_V4);

        assertEquals(WebSocket.NAME, webSocket.getName());
    }

    @Test
    public void testWritable() {
        final WebSocket webSocket = new WebSocket(Mockito.mock(EngineIoWebSocket.class), Parser.PROTOCOL_V4);

        assertTrue(webSocket.isWritable());
    }

    @Test
    public void testOnRequest() {
        final WebSocket webSocket = new WebSocket(Mockito.mock(EngineIoWebSocket.class), Parser.PROTOCOL_V4);
        webSocket.onRequest(Mockito.mock(HttpServletRequest.class), Mockito.mock(HttpServletResponse.class));
    }

    @Test
    public void testClose() {
        final EngineIoWebSocket webSocketConnection = Mockito.mock(EngineIoWebSocket.class);
        final WebSocket webSocket = Mockito.spy(new WebSocket(webSocketConnection, Parser.PROTOCOL_V4));

        webSocket.close();

        Mockito.verify(webSocket, Mockito.times(1))
                .doClose();
        Mockito.verify(webSocketConnection, Mockito.times(1))
                .close();
    }

    @Test
    public void testSend_string() {
        final EngineIoWebSocket webSocketConnection = Mockito.mock(EngineIoWebSocket.class);
        final WebSocket webSocket = Mockito.spy(new WebSocket(webSocketConnection, Parser.PROTOCOL_V4));

        final String stringData = "Test string";
        final Packet<?> packet = new Packet<>(Packet.MESSAGE, stringData);
        webSocket.send(new ArrayList<Packet<?>>() {{
            add(packet);
        }});

        Mockito.verify(webSocket, Mockito.times(1)).send(Mockito.anyList());

        Parser.PROTOCOL_V4.encodePacket(packet, true, data -> {
            try {
                Mockito.verify(webSocketConnection, Mockito.times(1))
                        .write(Mockito.eq((String) data));
            } catch (IOException ignore) {
            }
        });
    }

    @Test
    public void testSend_binary() {
        final EngineIoWebSocket webSocketConnection = Mockito.mock(EngineIoWebSocket.class);
        final WebSocket webSocket = Mockito.spy(new WebSocket(webSocketConnection, Parser.PROTOCOL_V4));

        final byte[] binaryData = "Test string".getBytes(StandardCharsets.UTF_8);
        final Packet<?> packet = new Packet<>(Packet.MESSAGE, binaryData);
        webSocket.send(new ArrayList<Packet<?>>() {{
            add(packet);
        }});

        Mockito.verify(webSocket, Mockito.times(1)).send(Mockito.anyList());

        Parser.PROTOCOL_V4.encodePacket(packet, true, data -> {
            try {
                Mockito.verify(webSocketConnection, Mockito.times(1))
                        .write(Mockito.eq((byte[]) data));
            } catch (IOException ignore) {
            }
        });
    }

    @Test
    public void testSend_error() throws IOException {
        final EngineIoWebSocket webSocketConnection = Mockito.mock(EngineIoWebSocket.class);
        final WebSocket webSocket = Mockito.spy(new WebSocket(webSocketConnection, Parser.PROTOCOL_V4));

        Mockito.doThrow(new IOException()).when(webSocketConnection).write(Mockito.anyString());
        webSocket.on("error", args -> { });

        final String stringData = "Test string";
        final Packet<?> packet = new Packet<>(Packet.MESSAGE, stringData);
        webSocket.send(new ArrayList<Packet<?>>() {{
            add(packet);
        }});

        Mockito.verify(webSocket, Mockito.times(1))
                .send(Mockito.anyList());
        Mockito.verify(webSocket, Mockito.times(1))
                .emit(Mockito.eq("error"), Mockito.eq("write error"), Mockito.isNull());
    }

    @Test
    public void testConnection_close() {
        final EngineIoWebSocket webSocketConnection = new EngineIoWebSocketStub();
        final WebSocket webSocket = Mockito.spy(new WebSocket(webSocketConnection, Parser.PROTOCOL_V4));

        final Emitter.Listener closeListener = Mockito.mock(Emitter.Listener.class);
        webSocket.on("close", closeListener);
        webSocketConnection.emit("close");

        Mockito.verify(closeListener, Mockito.times(1))
                .call();
    }

    @Test
    public void testConnection_error() {
        final EngineIoWebSocket webSocketConnection = Mockito.spy(new EngineIoWebSocketStub());
        final WebSocket webSocket = Mockito.spy(new WebSocket(webSocketConnection, Parser.PROTOCOL_V4));

        final Emitter.Listener errorListener = Mockito.mock(Emitter.Listener.class);
        webSocket.on("error", errorListener);
        webSocketConnection.emit("error", "test", null);

        Mockito.verify(errorListener, Mockito.times(1))
                .call(Mockito.eq("test"), Mockito.isNull());
    }

    @Test
    public void testConnection_message() {
        final EngineIoWebSocket webSocketConnection = Mockito.spy(new EngineIoWebSocketStub());
        final WebSocket webSocket = Mockito.spy(new WebSocket(webSocketConnection, Parser.PROTOCOL_V4));

        final Packet<String> packet = new Packet<>(Packet.MESSAGE, "Test Message");
        final Emitter.Listener packetListener = Mockito.mock(Emitter.Listener.class);
        Mockito.doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            @SuppressWarnings("unchecked") final Packet<?> argPacket = (Packet<Object>) args[0];
            assertEquals(packet.type, argPacket.type);
            assertEquals(packet.data, argPacket.data);
            return null;
        }).when(packetListener).call(Mockito.any());
        webSocket.on("packet", packetListener);

        Parser.PROTOCOL_V4.encodePacket(packet, true, data -> {
            webSocketConnection.emit("message", data);

            Mockito.verify(packetListener, Mockito.times(1))
                    .call(Mockito.any(Packet.class));
        });
    }
}