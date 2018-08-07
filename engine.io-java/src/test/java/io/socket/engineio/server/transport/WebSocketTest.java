package io.socket.engineio.server.transport;

import io.socket.emitter.Emitter;
import io.socket.engineio.parser.Packet;
import io.socket.engineio.parser.Parser;
import io.socket.engineio.parser.ServerParser;
import io.socket.engineio.server.EngineIoWebSocket;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
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
        final WebSocket webSocket = new WebSocket(Mockito.mock(EngineIoWebSocket.class));

        assertEquals(WebSocket.NAME, webSocket.getName());
    }

    @Test
    public void testWritable() {
        final WebSocket webSocket = new WebSocket(Mockito.mock(EngineIoWebSocket.class));

        assertTrue(webSocket.isWritable());
    }

    @Test
    public void testOnRequest() {
        final WebSocket webSocket = new WebSocket(Mockito.mock(EngineIoWebSocket.class));
        webSocket.onRequest(Mockito.mock(HttpServletRequest.class), Mockito.mock(HttpServletResponse.class));
    }

    @Test
    public void testClose() {
        final EngineIoWebSocket webSocketConnection = Mockito.mock(EngineIoWebSocket.class);
        final WebSocket webSocket = Mockito.spy(new WebSocket(webSocketConnection));

        webSocket.close();

        Mockito.verify(webSocket, Mockito.times(1))
                .doClose();
        Mockito.verify(webSocketConnection, Mockito.times(1))
                .close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSend_string() {
        final EngineIoWebSocket webSocketConnection = Mockito.mock(EngineIoWebSocket.class);
        final WebSocket webSocket = Mockito.spy(new WebSocket(webSocketConnection));

        final String stringData = "Test string";
        final Packet packet = new Packet(Packet.MESSAGE, stringData);
        webSocket.send(new ArrayList<Packet>() {{
            add(packet);
        }});

        Mockito.verify(webSocket, Mockito.times(1)).send(Mockito.<Packet>anyList());

        ServerParser.encodePacket(packet, true, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                try {
                    Mockito.verify(webSocketConnection, Mockito.times(1))
                            .write(Mockito.eq((String) data));
                } catch (IOException ignore) {
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSend_binary() {
        final EngineIoWebSocket webSocketConnection = Mockito.mock(EngineIoWebSocket.class);
        final WebSocket webSocket = Mockito.spy(new WebSocket(webSocketConnection));

        final byte[] binaryData = "Test string".getBytes(StandardCharsets.UTF_8);
        final Packet packet = new Packet(Packet.MESSAGE, binaryData);
        webSocket.send(new ArrayList<Packet>() {{
            add(packet);
        }});

        Mockito.verify(webSocket, Mockito.times(1)).send(Mockito.<Packet>anyList());

        ServerParser.encodePacket(packet, true, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                try {
                    Mockito.verify(webSocketConnection, Mockito.times(1))
                            .write(Mockito.eq((byte[]) data));
                } catch (IOException ignore) {
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSend_error() throws IOException {
        final EngineIoWebSocket webSocketConnection = Mockito.mock(EngineIoWebSocket.class);
        final WebSocket webSocket = Mockito.spy(new WebSocket(webSocketConnection));

        Mockito.doThrow(new IOException()).when(webSocketConnection).write(Mockito.anyString());
        webSocket.on("error", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
            }
        });

        final String stringData = "Test string";
        final Packet packet = new Packet(Packet.MESSAGE, stringData);
        webSocket.send(new ArrayList<Packet>() {{
            add(packet);
        }});

        Mockito.verify(webSocket, Mockito.times(1))
                .send(Mockito.<Packet>anyList());
        Mockito.verify(webSocket, Mockito.times(1))
                .emit(Mockito.eq("error"), Mockito.eq("write error"), Mockito.isNull());
    }

    @Test
    public void testConnection_close() {
        final EngineIoWebSocket webSocketConnection = new EngineIoWebSocketStub();
        final WebSocket webSocket = Mockito.spy(new WebSocket(webSocketConnection));

        final Emitter.Listener closeListener = Mockito.spy(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
            }
        });
        webSocket.on("close", closeListener);
        webSocketConnection.emit("close");

        Mockito.verify(closeListener, Mockito.times(1))
                .call();
    }

    @Test
    public void testConnection_error() {
        final EngineIoWebSocket webSocketConnection = Mockito.spy(new EngineIoWebSocketStub());
        final WebSocket webSocket = Mockito.spy(new WebSocket(webSocketConnection));

        final Emitter.Listener errorListener = Mockito.spy(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
            }
        });
        webSocket.on("error", errorListener);
        webSocketConnection.emit("error", "test", null);

        Mockito.verify(errorListener, Mockito.times(1))
                .call(Mockito.eq("test"), Mockito.isNull());
    }

    @Test
    public void testConnection_message() {
        final EngineIoWebSocket webSocketConnection = Mockito.spy(new EngineIoWebSocketStub());
        final WebSocket webSocket = Mockito.spy(new WebSocket(webSocketConnection));

        final Packet<String> packet = new Packet<>(Packet.MESSAGE, "Test Message");
        final Emitter.Listener packetListener = Mockito.spy(new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final Packet argPacket = (Packet) args[0];
                assertEquals(packet.type, argPacket.type);
                assertEquals(packet.data, argPacket.data);
            }
        });
        webSocket.on("packet", packetListener);

        ServerParser.encodePacket(packet, true, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                webSocketConnection.emit("message", data);

                Mockito.verify(packetListener, Mockito.times(1))
                        .call(Mockito.any(Packet.class));
            }
        });
    }
}