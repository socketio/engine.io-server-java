package io.socket.engineio.server;

import io.socket.emitter.Emitter;
import io.socket.engineio.server.transport.Polling;
import io.socket.parseqs.ParseQS;
import io.socket.yeast.Yeast;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public final class EngineIoServer extends Emitter {

    private static final EngineIoServer sInstance = new EngineIoServer();

    private final Map<String, EngineIoSocket> mClients = new TreeMap<>();

    private long mPingTimeout;
    private long mPingInterval;
    private long mUpgradeTimeout;

    private EngineIoServer() {
        mPingTimeout = 5000;
        mPingInterval = 25000;
        mUpgradeTimeout = 10000;
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

    public long getUpgradeTimeout() {
        return mUpgradeTimeout;
    }

    void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final Map<String, String> query = ParseQS.decode(request.getQueryString());
        request.setAttribute("query", query);

        if (query.containsKey("sid")) {
            if(!mClients.containsKey(query.get("sid"))) {
                sendErrorMessage(request, response, ServerErrors.UNKNOWN_SID);
                return;
            }
            if(!request.getMethod().toLowerCase().equals("upgrade") && false /* check transport name */) {
                sendErrorMessage(request, response, ServerErrors.BAD_REQUEST);
                return;
            }

            mClients.get(query.get("sid")).onRequest(request, response);
        } else {
            if(!request.getMethod().toLowerCase().equals("get")) {
                sendErrorMessage(request, response, ServerErrors.BAD_HANDSHAKE_METHOD);
                return;
            }

            handshake(request, response);
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

    private void handshake(HttpServletRequest request, HttpServletResponse response) throws IOException {
        @SuppressWarnings("unchecked") final Map<String, String> query = (Map<String, String>) request.getAttribute("query");
        final String sid = Yeast.yeast();

        Transport transport = null;

        switch (query.get("transport").toLowerCase()) {
            case Polling.NAME:
                transport = new Polling();
                break;
            default:
                sendErrorMessage(request, response, ServerErrors.BAD_REQUEST);
                break;
        }

        if(transport != null) {
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
    }
}