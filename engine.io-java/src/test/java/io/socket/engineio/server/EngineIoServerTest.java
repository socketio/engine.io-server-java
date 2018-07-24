package io.socket.engineio.server;

import io.socket.parseqs.ParseQS;
import io.socket.yeast.Yeast;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class EngineIoServerTest {

    @Test
    public void testHandleRequest_unknown_transport() throws IOException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                HashMap<String, String> queryMap = new HashMap<>();
                queryMap.put("transport", "invalid");

                return ParseQS.encode(queryMap);
            }
        }).when(request).getQueryString();

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return printWriter;
            }
        }).when(response).getWriter();

        final EngineIoServer server = new EngineIoServer();
        server.handleRequest(request, response);

        JSONObject responseObject = new JSONObject(stringWriter.toString());
        assertTrue(responseObject.has("code"));
        assertEquals(ServerErrors.UNKNOWN_TRANSPORT.getCode(), responseObject.get("code"));
        assertTrue(responseObject.has("message"));
        assertEquals(ServerErrors.UNKNOWN_TRANSPORT.getMessage(), responseObject.get("message"));
    }

    @Test
    public void testHandleRequest_bad_handshake_method() throws IOException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
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

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return printWriter;
            }
        }).when(response).getWriter();

        final EngineIoServer server = new EngineIoServer();
        server.handleRequest(request, response);

        JSONObject responseObject = new JSONObject(stringWriter.toString());
        assertTrue(responseObject.has("code"));
        assertEquals(ServerErrors.BAD_HANDSHAKE_METHOD.getCode(), responseObject.get("code"));
        assertTrue(responseObject.has("message"));
        assertEquals(ServerErrors.BAD_HANDSHAKE_METHOD.getMessage(), responseObject.get("message"));
    }

    @Test
    public void testHandleRequest_unknown_sid() throws IOException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                HashMap<String, String> queryMap = new HashMap<>();
                queryMap.put("transport", "polling");
                queryMap.put("sid", Yeast.yeast());

                return ParseQS.encode(queryMap);
            }
        }).when(request).getQueryString();

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) {
                return printWriter;
            }
        }).when(response).getWriter();

        final EngineIoServer server = new EngineIoServer();
        server.handleRequest(request, response);

        JSONObject responseObject = new JSONObject(stringWriter.toString());
        assertTrue(responseObject.has("code"));
        assertEquals(ServerErrors.UNKNOWN_SID.getCode(), responseObject.get("code"));
        assertTrue(responseObject.has("message"));
        assertEquals(ServerErrors.UNKNOWN_SID.getMessage(), responseObject.get("message"));
    }
}