package io.socket.engineio.server;

import io.socket.emitter.Emitter;
import io.socket.engineio.server.transport.Polling;
import io.socket.parseqs.ParseQS;
import io.socket.yeast.Yeast;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class EngineIoServerTest {

    private static final class WebSocketConnectionStub extends EngineIoWebSocket {

        private final Map<String, String> mQuery;

        WebSocketConnectionStub() {
            this(new HashMap<String, String>());
        }

        WebSocketConnectionStub(Map<String, String> query) {
            mQuery = query;
        }

        @Override
        public Map<String, String> getQuery() {
            return mQuery;
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
        assertEquals(1500, server.getPingInterval());
        assertEquals(1500, server.getPingTimeout());
    }

    @Test
    public void testHandleRequest_unknown_transport() throws IOException {
        final EngineIoServer server = new EngineIoServer();

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                HashMap<String, String> queryMap = new HashMap<>();
                queryMap.put("transport", "invalid");

                return ParseQS.encode(queryMap);
            }
        }).when(request).getQueryString();

        final HttpServletResponseImpl response = new HttpServletResponseImpl();

        server.handleRequest(request, response);

        response.flushWriterIfNecessary();
        final String responseStr = new String(response.getByteOutputStream().toByteArray(), StandardCharsets.UTF_8);
        final JSONObject responseObject = new JSONObject(responseStr);
        assertTrue(responseObject.has("code"));
        assertEquals(ServerErrors.UNKNOWN_TRANSPORT.getCode(), responseObject.get("code"));
        assertTrue(responseObject.has("message"));
        assertEquals(ServerErrors.UNKNOWN_TRANSPORT.getMessage(), responseObject.get("message"));
    }

    @Test
    public void testHandleRequest_bad_handshake_method() throws IOException {
        final EngineIoServer server = new EngineIoServer();

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                HashMap<String, String> queryMap = new HashMap<>();
                queryMap.put("transport", "polling");

                return ParseQS.encode(queryMap);
            }
        }).when(request).getQueryString();
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return "POST";
            }
        }).when(request).getMethod();

        final HttpServletResponseImpl response = new HttpServletResponseImpl();

        server.handleRequest(request, response);

        response.flushWriterIfNecessary();
        final String responseStr = new String(response.getByteOutputStream().toByteArray(), StandardCharsets.UTF_8);
        final JSONObject responseObject = new JSONObject(responseStr);
        assertTrue(responseObject.has("code"));
        assertEquals(ServerErrors.BAD_HANDSHAKE_METHOD.getCode(), responseObject.get("code"));
        assertTrue(responseObject.has("message"));
        assertEquals(ServerErrors.BAD_HANDSHAKE_METHOD.getMessage(), responseObject.get("message"));
    }

    @Test
    public void testHandleRequest_unknown_sid() throws IOException {
        final EngineIoServer server = new EngineIoServer();

        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                HashMap<String, String> queryMap = new HashMap<>();
                queryMap.put("transport", "polling");
                queryMap.put("sid", Yeast.yeast());

                return ParseQS.encode(queryMap);
            }
        }).when(request).getQueryString();

        final HttpServletResponseImpl response = new HttpServletResponseImpl();

        server.handleRequest(request, response);

        response.flushWriterIfNecessary();
        final String responseStr = new String(response.getByteOutputStream().toByteArray(), StandardCharsets.UTF_8);
        final JSONObject responseObject = new JSONObject(responseStr);
        assertTrue(responseObject.has("code"));
        assertEquals(ServerErrors.UNKNOWN_SID.getCode(), responseObject.get("code"));
        assertTrue(responseObject.has("message"));
        assertEquals(ServerErrors.UNKNOWN_SID.getMessage(), responseObject.get("message"));
    }

    @Test
    public void testHandleRequest_connect() throws IOException {
        final EngineIoServer server = new EngineIoServer();

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

    private HttpServletRequest getConnectRequest(final Map<String, String> query) {
        final HashMap<String, Object> attributes = new HashMap<>();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return ParseQS.encode(query);
            }
        }).when(request).getQueryString();
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return "GET";
            }
        }).when(request).getMethod();
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                final String name = invocationOnMock.getArgument(0);
                final Object value = invocationOnMock.getArgument(1);
                attributes.put(name, value);
                return null;
            }
        }).when(request).setAttribute(Mockito.anyString(), Mockito.any());
        Mockito.doAnswer(new Answer() {
            @SuppressWarnings("SuspiciousMethodCalls")
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return attributes.get(invocationOnMock.getArgument(0));
            }
        }).when(request).getAttribute(Mockito.anyString());
        return request;
    }
}