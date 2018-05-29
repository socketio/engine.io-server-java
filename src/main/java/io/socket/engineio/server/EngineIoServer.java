package io.socket.engineio.server;

import io.socket.emitter.Emitter;
import io.socket.engineio.server.transport.Polling;
import io.socket.engineio.server.transport.WebSocket;
import io.socket.parseqs.ParseQS;
import io.socket.yeast.Yeast;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("WeakerAccess")
public final class EngineIoServer extends Emitter {

    private static final EngineIoServer sInstance = new EngineIoServer();

    private final Map<String, EngineIoSocket> mClients = new TreeMap<>();

    private long mPingTimeout;
    private long mPingInterval;

    private EngineIoServer() {
        mPingTimeout = 5000;
        mPingInterval = 25000;
    }

    public static EngineIoServer getInstance() {
        return sInstance;
    }

    public long getPingTimeout() {
        return mPingTimeout;
    }

    public long getPingInterval() {
        return mPingInterval;
    }

    void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final Map<String, String> query = ParseQS.decode(request.getQueryString());
        request.setAttribute("query", query);

        final String sid = query.containsKey("sid")? query.get("sid") : null;
        if (sid != null) {
            if(!mClients.containsKey(query.get("sid"))) {
                sendErrorMessage(request, response, ServerErrors.UNKNOWN_SID);
            } else if(!query.containsKey("transport") ||
                            !query.get("transport").equals(mClients.get(sid).getCurrentTransportName())) {
                sendErrorMessage(request, response, ServerErrors.BAD_REQUEST);
            } else {
                mClients.get(sid).onRequest(request, response);
            }
        } else {
            if(!request.getMethod().toUpperCase().equals("GET")) {
                sendErrorMessage(request, response, ServerErrors.BAD_HANDSHAKE_METHOD);
            } else {
                handshakePolling(request, response);
            }
        }
    }

    void handleWebSocket(EngineIoWebSocket webSocket) {
        final Map<String, String> query = webSocket.getQuery();
        final String sid = query.containsKey("sid")? query.get("sid") : null;

        if(sid != null) {
            EngineIoSocket socket = mClients.get(sid);
            if(socket == null) {
                webSocket.close();
            } else if(!socket.canUpgrade(WebSocket.NAME)) {
                webSocket.close();
            } else {
                Transport transport = new WebSocket(webSocket);
                socket.upgrade(transport);
            }
        } else {
            handshakeWebSocket(webSocket);
        }
    }

    private void sendErrorMessage(HttpServletRequest request, HttpServletResponse response, ServerErrors code) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        if(code != null) {
            response.setStatus(400);

            final String origin = request.getHeader("Origin");
            if(origin != null) {
                response.addHeader("Access-Control-Allow-Credentials", "true");
                response.addHeader("Access-Control-Allow-Origin", origin);
            } else {
                response.addHeader("Access-Control-Allow-Origin", "*");
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.append("code", code.getCode());
            jsonObject.append("message", code.getMessage());
            response.getWriter().write(jsonObject.toString());
        } else {
            response.setStatus(403);

            JSONObject jsonObject = new JSONObject();
            jsonObject.append("code", ServerErrors.FORBIDDEN.getCode());
            jsonObject.append("message", ServerErrors.FORBIDDEN.getMessage());
            response.getWriter().write(jsonObject.toString());
        }
    }

    private void handshakePolling(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String sid = Yeast.yeast();

        Transport transport = new Polling();
        EngineIoSocket socket = new EngineIoSocket(sid, EngineIoServer.getInstance(), transport, request);
        transport.onRequest(request, response);

        mClients.put(sid, socket);
        socket.once("close", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                mClients.remove(sid);
            }
        });

        EngineIoServer.getInstance().emit("connection", socket);
    }

    private void handshakeWebSocket(EngineIoWebSocket webSocket) {
        final String sid = Yeast.yeast();

        Transport transport = new WebSocket(webSocket);
        EngineIoSocket socket = new EngineIoSocket(sid, EngineIoServer.getInstance(), transport, null);

        mClients.put(sid, socket);
        socket.once("close", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                mClients.remove(sid);
            }
        });

        EngineIoServer.getInstance().emit("connection", socket);
    }
}