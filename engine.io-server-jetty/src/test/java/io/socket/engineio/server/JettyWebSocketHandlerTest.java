package io.socket.engineio.server;

import io.socket.engineio.server.transport.WebSocket;
import io.socket.engineio.server.utils.ParseQS;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Random;

public final class JettyWebSocketHandlerTest {

    @Test
    public void testOnConnect() {
        final String queryString = ParseQS.encode(new HashMap<String, String>() {{
            put("transport", WebSocket.NAME);
        }});
        final UpgradeRequest upgradeRequest = Mockito.mock(UpgradeRequest.class);
        final Session session = Mockito.mock(Session.class);
        final EngineIoServer server = Mockito.spy(new EngineIoServer());

        Mockito.doAnswer(invocationOnMock -> queryString).when(upgradeRequest).getQueryString();
        Mockito.doAnswer(invocationOnMock -> upgradeRequest).when(session).getUpgradeRequest();
        Mockito.doAnswer(invocationOnMock -> null).when(server).handleWebSocket(Mockito.any(EngineIoWebSocket.class));

        final JettyWebSocketHandler handler = new JettyWebSocketHandler(server);
        handler.onWebSocketConnect(session);

        Mockito.verify(server, Mockito.times(1))
                .handleWebSocket(Mockito.eq(handler));
        Assert.assertNotNull(handler.getQuery());
        Assert.assertTrue(handler.getQuery().containsKey("transport"));
        Assert.assertEquals(handler.getQuery().get("transport"), WebSocket.NAME);
    }

    @Test
    public void testOnClose() {
        final EngineIoServer server = new EngineIoServer();

        final JettyWebSocketHandler handler = Mockito.spy(new JettyWebSocketHandler(server));
        handler.onWebSocketClose(0, "test");

        Mockito.verify(handler, Mockito.times(1))
                .emit(Mockito.eq("close"));
    }

    @Test
    public void testOnError() {
        final String errorMessage = "Test Exception";
        final EngineIoServer server = new EngineIoServer();

        final JettyWebSocketHandler handler = Mockito.spy(new JettyWebSocketHandler(server));
        handler.onWebSocketError(new Exception(errorMessage));

        Mockito.verify(handler, Mockito.times(1))
                .emit(Mockito.eq("error"), Mockito.eq("write error"), Mockito.eq(errorMessage));
    }

    @Test
    public void testOnMessage_binary_full() {
        final byte[] message = new byte[16];
        (new Random()).nextBytes(message);

        final EngineIoServer server = new EngineIoServer();

        final JettyWebSocketHandler handler = Mockito.spy(new JettyWebSocketHandler(server));
        handler.onWebSocketBinary(message, 0, message.length);

        Mockito.verify(handler, Mockito.times(1))
                .emit(Mockito.eq("message"), (Object) Mockito.eq(message));
    }

    @Test
    public void testOnMessage_binary_partial() {
        final byte[] message = new byte[16];
        (new Random()).nextBytes(message);

        final byte[] messagePart = new byte[10];
        final int offset = 3;
        System.arraycopy(message, offset, messagePart, 0, messagePart.length);

        final EngineIoServer server = new EngineIoServer();

        final JettyWebSocketHandler handler = Mockito.spy(new JettyWebSocketHandler(server));
        handler.onWebSocketBinary(message, offset, messagePart.length);

        Mockito.verify(handler, Mockito.times(1))
                .emit(Mockito.eq("message"), (Object) Mockito.eq(messagePart));
    }

    @Test
    public void testOnMessage_string() {
        final String message = "FooBarBaz";
        final EngineIoServer server = new EngineIoServer();

        final JettyWebSocketHandler handler = Mockito.spy(new JettyWebSocketHandler(server));
        handler.onWebSocketText(message);

        Mockito.verify(handler, Mockito.times(1))
                .emit(Mockito.eq("message"), Mockito.eq(message));
    }

    @Test
    public void testSocket_close() {
        final String queryString = ParseQS.encode(new HashMap<String, String>() {{
            put("transport", WebSocket.NAME);
        }});
        final UpgradeRequest upgradeRequest = Mockito.mock(UpgradeRequest.class);
        final Session session = Mockito.mock(Session.class);
        final EngineIoServer server = Mockito.spy(new EngineIoServer());

        Mockito.doAnswer(invocationOnMock -> queryString).when(upgradeRequest).getQueryString();
        Mockito.doAnswer(invocationOnMock -> upgradeRequest).when(session).getUpgradeRequest();
        Mockito.doAnswer(invocationOnMock -> null).when(server).handleWebSocket(Mockito.any(EngineIoWebSocket.class));

        final JettyWebSocketHandler handler = new JettyWebSocketHandler(server);
        handler.onWebSocketConnect(session);

        handler.close();
        Mockito.verify(session, Mockito.times(1))
                .close();
    }

    @Test
    public void testSocket_write_string() throws IOException {
        final String queryString = ParseQS.encode(new HashMap<String, String>() {{
            put("transport", WebSocket.NAME);
        }});
        final UpgradeRequest upgradeRequest = Mockito.mock(UpgradeRequest.class);
        final RemoteEndpoint remoteEndpoint = Mockito.mock(RemoteEndpoint.class);
        final Session session = Mockito.mock(Session.class);
        final EngineIoServer server = Mockito.spy(new EngineIoServer());

        Mockito.doAnswer(invocationOnMock -> queryString).when(upgradeRequest).getQueryString();
        Mockito.doAnswer(invocationOnMock -> upgradeRequest).when(session).getUpgradeRequest();
        Mockito.doAnswer(invocationOnMock -> remoteEndpoint).when(session).getRemote();
        Mockito.doAnswer(invocationOnMock -> null).when(server).handleWebSocket(Mockito.any(EngineIoWebSocket.class));

        final JettyWebSocketHandler handler = new JettyWebSocketHandler(server);
        handler.onWebSocketConnect(session);

        final String message = "FooBar";
        handler.write(message);
        Mockito.verify(remoteEndpoint, Mockito.times(1))
                .sendString(Mockito.eq(message));
    }

    @Test
    public void testSocket_write_binary() throws IOException {
        final String queryString = ParseQS.encode(new HashMap<String, String>() {{
            put("transport", WebSocket.NAME);
        }});
        final UpgradeRequest upgradeRequest = Mockito.mock(UpgradeRequest.class);
        final RemoteEndpoint remoteEndpoint = Mockito.mock(RemoteEndpoint.class);
        final Session session = Mockito.mock(Session.class);
        final EngineIoServer server = Mockito.spy(new EngineIoServer());

        Mockito.doAnswer(invocationOnMock -> queryString).when(upgradeRequest).getQueryString();
        Mockito.doAnswer(invocationOnMock -> upgradeRequest).when(session).getUpgradeRequest();
        Mockito.doAnswer(invocationOnMock -> remoteEndpoint).when(session).getRemote();
        Mockito.doAnswer(invocationOnMock -> null).when(server).handleWebSocket(Mockito.any(EngineIoWebSocket.class));

        final JettyWebSocketHandler handler = new JettyWebSocketHandler(server);
        handler.onWebSocketConnect(session);

        final byte[] message = new byte[16];
        (new Random()).nextBytes(message);
        handler.write(message);
        Mockito.verify(remoteEndpoint, Mockito.times(1))
                .sendBytes(Mockito.any(ByteBuffer.class));
    }
}