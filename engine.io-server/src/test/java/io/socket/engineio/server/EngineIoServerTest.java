package io.socket.engineio.server;

import io.socket.engineio.server.transport.Polling;
import io.socket.engineio.server.utils.ParseQS;
import io.socket.engineio.server.utils.ServerYeast;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class EngineIoServerTest {

    private static final class WebSocketConnectionStub extends EngineIoWebSocket {

        private final Map<String, String> mQuery;
        private final Map<String, List<String>> mHeaders;

        WebSocketConnectionStub() {
            this(new HashMap<>(), new HashMap<>());
        }

        WebSocketConnectionStub(Map<String, String> query, Map<String, List<String>> headers) {
            mQuery = query;
            mHeaders = headers;
        }

        @Override
        public Map<String, String> getQuery() {
            return mQuery;
        }

        @Override
        public Map<String, List<String>> getConnectionHeaders() {
            return mHeaders;
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
    public void testOptions() {
        EngineIoServer server = new EngineIoServer(EngineIoServerOptions.newFromDefault()
                .setPingInterval(1500)
                .setPingTimeout(1500));
        assertEquals(1500, server.getOptions().getPingInterval());
        assertEquals(1500, server.getOptions().getPingTimeout());
    }

    @Test
    public void testHandleRequest_unknown_transport() throws IOException {
        final EngineIoServer server = new EngineIoServer();

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(invocationOnMock -> {
            HashMap<String, String> queryMap = new HashMap<>();
            queryMap.put("transport", "invalid");

            return ParseQS.encode(queryMap);
        }).when(request).getQueryString();

        final HttpServletResponseImpl response = new HttpServletResponseImpl();

        server.handleRequest(request, response);

        response.flushWriterIfNecessary();
        final String responseStr = new String(response.getByteOutputStream().toByteArray(), StandardCharsets.UTF_8);
        final JSONObject responseObject = new JSONObject(responseStr);
        assertTrue(responseObject.has("code"));
        assertEquals(ServerErrors.UNKNOWN_TRANSPORT.getCode(), (int) responseObject.get("code"));
        assertTrue(responseObject.has("message"));
        assertEquals(ServerErrors.UNKNOWN_TRANSPORT.getMessage(), responseObject.get("message"));
    }

    @Test
    public void testHandleRequest_bad_handshake_method() throws IOException {
        final EngineIoServer server = new EngineIoServer(EngineIoServerOptions.newFromDefault()
                .setAllowSyncPolling(true));

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(invocationOnMock -> {
            HashMap<String, String> queryMap = new HashMap<>();
            queryMap.put("transport", "polling");

            return ParseQS.encode(queryMap);
        }).when(request).getQueryString();
        Mockito.doAnswer(invocationOnMock -> "POST").when(request).getMethod();

        final HttpServletResponseImpl response = new HttpServletResponseImpl();

        server.handleRequest(request, response);

        response.flushWriterIfNecessary();
        final String responseStr = new String(response.getByteOutputStream().toByteArray(), StandardCharsets.UTF_8);
        final JSONObject responseObject = new JSONObject(responseStr);
        assertTrue(responseObject.has("code"));
        assertEquals(ServerErrors.BAD_HANDSHAKE_METHOD.getCode(), (int) responseObject.get("code"));
        assertTrue(responseObject.has("message"));
        assertEquals(ServerErrors.BAD_HANDSHAKE_METHOD.getMessage(), responseObject.get("message"));
    }

    @Test
    public void testHandleRequest_unknown_sid() throws IOException {
        final EngineIoServer server = new EngineIoServer(EngineIoServerOptions.newFromDefault()
                .setAllowSyncPolling(true));

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(invocationOnMock -> {
            HashMap<String, String> queryMap = new HashMap<>();
            queryMap.put("transport", "polling");
            queryMap.put("sid", ServerYeast.yeast());

            return ParseQS.encode(queryMap);
        }).when(request).getQueryString();

        final HttpServletResponseImpl response = new HttpServletResponseImpl();

        server.handleRequest(request, response);

        response.flushWriterIfNecessary();
        final String responseStr = new String(response.getByteOutputStream().toByteArray(), StandardCharsets.UTF_8);
        final JSONObject responseObject = new JSONObject(responseStr);
        assertTrue(responseObject.has("code"));
        assertEquals(ServerErrors.UNKNOWN_SID.getCode(), (int) responseObject.get("code"));
        assertTrue(responseObject.has("message"));
        assertEquals(ServerErrors.UNKNOWN_SID.getMessage(), responseObject.get("message"));
    }

    @Test
    public void testHandleRequest_connect() throws IOException {
        final EngineIoServer server = new EngineIoServer(EngineIoServerOptions.newFromDefault()
                .setAllowSyncPolling(true));

        final HttpServletRequest request = getConnectRequest(new HashMap<String, String>() {{
            put("transport", Polling.NAME);
        }});
        final HttpServletResponseImpl response = new HttpServletResponseImpl();

        final Emitter.Listener connectionListener = Mockito.mock(Emitter.Listener.class);
        server.on("connection", connectionListener);

        server.handleRequest(request, response);

        Mockito.verify(connectionListener, Mockito.times(1))
                .call(Mockito.any(EngineIoSocket.class));
    }

    @Test
    public void testHandleWebSocket_connect() {
        final EngineIoServer server = new EngineIoServer();

        final EngineIoWebSocket webSocket = new WebSocketConnectionStub();

        final Emitter.Listener connectionListener = Mockito.mock(Emitter.Listener.class);
        server.on("connection", connectionListener);

        server.handleWebSocket(webSocket);

        Mockito.verify(connectionListener, Mockito.times(1))
                .call(Mockito.any(EngineIoSocket.class));
    }

    @Test
    public void testCors_all() throws IOException {
        final String origin = "http://www.example.com";
        final EngineIoServer server = new EngineIoServer(EngineIoServerOptions.newFromDefault()
                .setAllowedCorsOrigins(EngineIoServerOptions.ALLOWED_CORS_ORIGIN_ALL));

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(invocationOnMock -> {
            HashMap<String, String> queryMap = new HashMap<>();
            queryMap.put("transport", "polling");

            return ParseQS.encode(queryMap);
        }).when(request).getQueryString();
        Mockito.doAnswer(invocationOnMock -> "POST").when(request).getMethod();
        Mockito.doAnswer(invocationOnMock -> origin).when(request).getHeader(Mockito.eq("Origin"));

        final HttpServletResponseImpl response = Mockito.spy(new HttpServletResponseImpl());

        server.handleRequest(request, response);

        Mockito.verify(response, Mockito.times(1))
                .addHeader(Mockito.eq("Access-Control-Allow-Origin"), Mockito.eq(origin));
    }

    @Test
    public void testCors_none() throws IOException {
        final String origin = "http://www.example.com";
        final EngineIoServer server = new EngineIoServer(EngineIoServerOptions.newFromDefault()
                .setAllowedCorsOrigins(EngineIoServerOptions.ALLOWED_CORS_ORIGIN_NONE));

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(invocationOnMock -> {
            HashMap<String, String> queryMap = new HashMap<>();
            queryMap.put("transport", "polling");

            return ParseQS.encode(queryMap);
        }).when(request).getQueryString();
        Mockito.doAnswer(invocationOnMock -> "POST").when(request).getMethod();
        Mockito.doAnswer(invocationOnMock -> origin).when(request).getHeader(Mockito.eq("Origin"));

        final HttpServletResponseImpl response = Mockito.spy(new HttpServletResponseImpl());

        server.handleRequest(request, response);

        Mockito.verify(response, Mockito.times(0))
                .addHeader(Mockito.eq("Access-Control-Allow-Origin"), Mockito.eq(origin));
    }

    @Test
    public void testCors_some1() throws IOException {
        final String origin = "http://www.example.com";
        final EngineIoServer server = new EngineIoServer(EngineIoServerOptions.newFromDefault()
                .setAllowedCorsOrigins(new String[] {
                        origin
                }));

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(invocationOnMock -> {
            HashMap<String, String> queryMap = new HashMap<>();
            queryMap.put("transport", "polling");

            return ParseQS.encode(queryMap);
        }).when(request).getQueryString();
        Mockito.doAnswer(invocationOnMock -> "POST").when(request).getMethod();
        Mockito.doAnswer(invocationOnMock -> origin).when(request).getHeader(Mockito.eq("Origin"));

        final HttpServletResponseImpl response = Mockito.spy(new HttpServletResponseImpl());

        server.handleRequest(request, response);

        Mockito.verify(response, Mockito.times(1))
                .addHeader(Mockito.eq("Access-Control-Allow-Origin"), Mockito.eq(origin));
    }

    @Test
    public void testCors_some2() throws IOException {
        final String origin = "http://www.example.com";
        final EngineIoServer server = new EngineIoServer(EngineIoServerOptions.newFromDefault()
                .setAllowedCorsOrigins(new String[] {
                        origin
                }));

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(invocationOnMock -> {
            HashMap<String, String> queryMap = new HashMap<>();
            queryMap.put("transport", "polling");

            return ParseQS.encode(queryMap);
        }).when(request).getQueryString();
        Mockito.doAnswer(invocationOnMock -> "POST").when(request).getMethod();
        Mockito.doAnswer(invocationOnMock -> "http://www.example.org").when(request).getHeader(Mockito.eq("Origin"));

        final HttpServletResponseImpl response = Mockito.spy(new HttpServletResponseImpl());

        server.handleRequest(request, response);

        Mockito.verify(response, Mockito.times(0))
                .addHeader(Mockito.eq("Access-Control-Allow-Origin"), Mockito.eq(origin));
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    private HttpServletRequest getConnectRequest(final Map<String, String> query) {
        final HashMap<String, Object> attributes = new HashMap<>();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(invocationOnMock -> ParseQS.encode(query)).when(request).getQueryString();
        Mockito.doAnswer(invocationOnMock -> "GET").when(request).getMethod();
        Mockito.doAnswer(invocationOnMock -> {
            final String name = invocationOnMock.getArgument(0);
            final Object value = invocationOnMock.getArgument(1);
            attributes.put(name, value);
            return null;
        }).when(request).setAttribute(Mockito.anyString(), Mockito.any());
        Mockito.doAnswer(invocationOnMock -> attributes.get(invocationOnMock.getArgument(0))).when(request).getAttribute(Mockito.anyString());
        return request;
    }
}