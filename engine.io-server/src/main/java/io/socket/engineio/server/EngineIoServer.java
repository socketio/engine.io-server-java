package io.socket.engineio.server;

import io.socket.emitter.Emitter;
import io.socket.engineio.server.transport.Polling;
import io.socket.engineio.server.transport.WebSocket;
import io.socket.parseqs.ParseQS;
import io.socket.yeast.ServerYeast;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The engine.io server.
 *
 * This class is responsible for handling all HTTP and WebSocket requests.
 */
@SuppressWarnings("WeakerAccess")
public final class EngineIoServer extends Emitter {

    private final Map<String, EngineIoSocket> mClients = new ConcurrentHashMap<>();
    private final EngineIoServerOptions mOptions;
    private final ScheduledExecutorService mScheduledExecutor;

    /**
     * Create instance of server with default options.
     */
    public EngineIoServer() {
        this(EngineIoServerOptions.DEFAULT);
    }

    /**
     * Create instance of server with specified options.
     * The options instance is locked to prevent further modifications.
     *
     * @param options Server options.
     */
    public EngineIoServer(EngineIoServerOptions options) {
        mOptions = options;
        mOptions.lock();

        mScheduledExecutor = Executors.newScheduledThreadPool(mOptions.getMaxTimeoutThreadPoolSize(), new ThreadFactory() {

            private final AtomicLong mThreadCount = new AtomicLong(0);

            @SuppressWarnings("NullableProblems")
            @Override
            public Thread newThread(Runnable runnable) {
                final Thread thread = new Thread(runnable);
                thread.setName(String.format("engineIo-threadPool-%d", mThreadCount.incrementAndGet()));
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    /**
     * Gets the configured options for this server instance.
     */
    public EngineIoServerOptions getOptions() {
        return mOptions;
    }

    /**
     * Gets the underlying executor used for ping timeout handling.
     */
    public ScheduledExecutorService getScheduledExecutor() {
        return mScheduledExecutor;
    }

    /**
     * Handle an HTTP request.
     *
     * This method handles polling transport connections.
     *
     * @param request The HTTP request object.
     * @param response The HTTP response object.
     * @throws IOException On IO error.
     */
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final Map<String, String> query = ParseQS.decode(request.getQueryString());
        request.setAttribute("query", query);

        if (!mOptions.isCorsHandlingDisabled()) {
            /*
             * Send cors headers if:
             * 1. 'Origin' header is present in request
             * 2. All origins are allowed (mOptions.getAllowedCorsOrigins() == EngineIoServerOptions.ALLOWED_CORS_ORIGIN_ALL)
             * 3. Specified origin is allowed (Arrays.binarySearch(mOptions.getAllowedCorsOrigins(), origin) >= 0)
             * */
            final String origin = request.getHeader("Origin");
            boolean sendCors = (origin != null) &&
                    ((mOptions.getAllowedCorsOrigins() == EngineIoServerOptions.ALLOWED_CORS_ORIGIN_ALL) ||
                            (Arrays.binarySearch(mOptions.getAllowedCorsOrigins(), origin) >= 0));
            if (sendCors) {
                response.addHeader("Access-Control-Allow-Origin", origin);
                response.addHeader("Access-Control-Allow-Credentials", "true");
                response.addHeader("Access-Control-Allow-Methods", "GET,HEAD,PUT,PATCH,POST,DELETE");
                response.addHeader("Access-Control-Allow-Headers", "origin, content-type, accept");
            }
        }

        if (!query.containsKey("transport") || !query.get("transport").equals("polling")) {
            sendErrorMessage(response, ServerErrors.UNKNOWN_TRANSPORT);
            return;
        }

        final String sid = query.get("sid");
        if (sid != null) {
            if(!mClients.containsKey(query.get("sid"))) {
                sendErrorMessage(response, ServerErrors.UNKNOWN_SID);
            } else if(!query.containsKey("transport") ||
                            !query.get("transport").equals(mClients.get(sid).getCurrentTransportName())) {
                sendErrorMessage(response, ServerErrors.BAD_REQUEST);
            } else {
                mClients.get(sid).onRequest(request, response);
            }
        } else {
            if(!request.getMethod().equalsIgnoreCase("GET")) {
                sendErrorMessage(response, ServerErrors.BAD_HANDSHAKE_METHOD);
            } else {
                handshakePolling(request, response);
            }
        }
    }

    /**
     * Handle a WebSocket request.
     *
     * This method handles websocket transport connections.
     *
     * @param webSocket The WebSocket connection object.
     */
    public void handleWebSocket(EngineIoWebSocket webSocket) {
        final Map<String, String> query = webSocket.getQuery();
        final String sid = query.get("sid");

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

    private void sendErrorMessage(HttpServletResponse response,
                                  ServerErrors code) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        if(code != null) {
            response.setStatus(400);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("code", code.getCode());
            jsonObject.put("message", code.getMessage());
            response.getWriter().write(jsonObject.toString());
        } else {
            response.setStatus(403);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("code", ServerErrors.FORBIDDEN.getCode());
            jsonObject.put("message", ServerErrors.FORBIDDEN.getMessage());
            response.getWriter().write(jsonObject.toString());
        }
    }

    private void handshakePolling(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String sid = ServerYeast.yeast();

        final Object lockObject = new Object();
        final EngineIoSocket socket = new EngineIoSocket(lockObject, sid, this, mScheduledExecutor);
        final Transport transport = new Polling(lockObject);
        socket.init(transport, request);
        transport.onRequest(request, response);
        socket.updateInitialHeadersFromActiveTransport();

        mClients.put(sid, socket);
        socket.once("close", args -> mClients.remove(sid));

        emit("connection", socket);
    }

    private void handshakeWebSocket(EngineIoWebSocket webSocket) {
        final String sid = ServerYeast.yeast();

        final Transport transport = new WebSocket(webSocket);
        final EngineIoSocket socket = new EngineIoSocket(new Object(), sid, this, mScheduledExecutor);
        socket.init(transport, null);

        mClients.put(sid, socket);
        socket.once("close", args -> mClients.remove(sid));

        emit("connection", socket);
    }
}