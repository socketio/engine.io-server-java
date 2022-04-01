package io.socket.engineio.server;

import io.socket.engineio.server.parser.Parser;
import io.socket.engineio.server.transport.Polling;
import io.socket.engineio.server.transport.WebSocket;
import io.socket.engineio.server.utils.JsonUtils;
import io.socket.engineio.server.utils.ParseQS;
import io.socket.engineio.server.utils.ServerYeast;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The engine.io server.
 *
 * This class is responsible for handling all HTTP and WebSocket requests.
 */
@SuppressWarnings("WeakerAccess")
public final class EngineIoServer extends Emitter {

    public interface HandshakeInterceptor {

        /**
         * Intercept and either allow or block the connection.
         * If blocked, a bad request error is returned to the client.
         *
         * @param query Query parameters of the connection.
         * @param headers Headers in the connection request.
         * @return Return true to allow the connection or false to block.
         */
        boolean intercept(Map<String, String> query, Map<String, List<String>> headers);
    }

    private static final String ERROR_JSON = "{\"code\": %d, \"message\": \"%s\"}";

    private final Map<String, EngineIoSocket> mClients = new ConcurrentHashMap<>();
    private final EngineIoServerOptions mOptions;
    private final HashSet<String> mAllowedCorsOrigins;
    private final ScheduledExecutorService mScheduledExecutor;
    private final HandshakeInterceptor mHandshakeInterceptor;

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

        if (options.getAllowedCorsOrigins() != null) {
            mAllowedCorsOrigins = new HashSet<>();
            for (String origin : options.getAllowedCorsOrigins()) {
                if (origin != null) {
                    mAllowedCorsOrigins.add(origin.toLowerCase(Locale.getDefault()));
                }
            }
        } else {
            mAllowedCorsOrigins = null;
        }

        if (mOptions.getScheduledExecutorService() != null) {
            mScheduledExecutor = mOptions.getScheduledExecutorService();
        } else {
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

        mHandshakeInterceptor = mOptions.getHandshakeInterceptor();
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
     * Releases resources such as threads and clients.
     */
    public void shutdown() {
        if (mOptions.getScheduledExecutorService() == null) {
            // Only call shutdown if we created this executor service
            mScheduledExecutor.shutdownNow();
        }
        mClients.clear();
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
                    ((mAllowedCorsOrigins == null) || mAllowedCorsOrigins.contains(origin.toLowerCase(Locale.getDefault())));
            if (sendCors) {
                response.addHeader("Access-Control-Allow-Origin", origin);
                response.addHeader("Access-Control-Allow-Credentials", "true");
                response.addHeader("Access-Control-Allow-Methods", "GET,HEAD,PUT,PATCH,POST,DELETE");
                response.addHeader("Access-Control-Allow-Headers", "origin, content-type, accept");
            }
        }

        final String transport = query.get("transport");
        if (transport == null || !transport.equals("polling")) {
            sendErrorMessage(response, ServerErrors.UNKNOWN_TRANSPORT);
            return;
        }

        if (!request.isAsyncSupported() && !mOptions.isSyncPollingAllowed()) {
            sendErrorMessage(response, ServerErrors.FORBIDDEN);
            return;
        }

        final String sid = query.get("sid");
        if (sid != null) {
            final EngineIoSocket client = mClients.get(sid);
            if(client == null) {
                sendErrorMessage(response, ServerErrors.UNKNOWN_SID);
            } else if(!transport.equals(client.getCurrentTransportName())) {
                sendErrorMessage(response, ServerErrors.BAD_REQUEST);
            } else {
                client.onRequest(request, response);
            }
        } else {
            if(!request.getMethod().equalsIgnoreCase("GET")) {
                sendErrorMessage(response, ServerErrors.BAD_HANDSHAKE_METHOD);
            } else {
                if (mHandshakeInterceptor != null) {
                    final HashMap<String, List<String>> headers = new HashMap<>();
                    final Enumeration<String> headerNamesEnum = request.getHeaderNames();
                    while (headerNamesEnum.hasMoreElements()) {
                        final String headerName = headerNamesEnum.nextElement();
                        headers.put(headerName, Collections.list(request.getHeaders(headerName)));
                    }
                    if (mHandshakeInterceptor.intercept(query, headers)) {
                        handshakePolling(request, response);
                    } else {
                        sendErrorMessage(response, ServerErrors.BAD_REQUEST);
                    }
                } else {
                    handshakePolling(request, response);
                }
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
                final Transport transport = new WebSocket(webSocket, parserFromQuery(webSocket.getQuery()));
                socket.upgrade(transport);
            }
        } else {
            if (mHandshakeInterceptor == null ||
                    mHandshakeInterceptor.intercept(webSocket.getQuery(), webSocket.getConnectionHeaders())) {
                handshakeWebSocket(webSocket);
            } else {
                webSocket.close();
            }
        }
    }

    private void sendErrorMessage(HttpServletResponse response,
                                  ServerErrors code) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        if(code != null) {
            response.setStatus(400);
            response.getWriter().write(String.format(
                    ERROR_JSON,
                    code.getCode(),
                    JsonUtils.escape(code.getMessage())));
        } else {
            response.setStatus(403);
            response.getWriter().write(String.format(
                    ERROR_JSON,
                    ServerErrors.FORBIDDEN.getCode(),
                    JsonUtils.escape(ServerErrors.FORBIDDEN.getMessage())));
        }
    }

    @SuppressWarnings("unchecked")
    private void handshakePolling(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String sid = ServerYeast.yeast();

        final Object lockObject = new Object();
        final Parser parser = parserFromQuery((Map<String, String>) request.getAttribute("query"));
        final EngineIoSocket socket = new EngineIoSocket(lockObject, sid, parser.getProtocolVersion(), this, mScheduledExecutor);
        final Transport transport = new Polling(lockObject, parser);
        socket.init(transport);
        transport.onRequest(request, response);
        socket.updateInitialHeadersFromActiveTransport();

        mClients.put(sid, socket);
        socket.once("close", args -> mClients.remove(sid));

        emit("connection", socket);
    }

    private void handshakeWebSocket(EngineIoWebSocket webSocket) {
        final String sid = ServerYeast.yeast();

        final Parser parser = parserFromQuery(webSocket.getQuery());
        final Transport transport = new WebSocket(webSocket, parser);
        final EngineIoSocket socket = new EngineIoSocket(new Object(), sid, parser.getProtocolVersion(), this, mScheduledExecutor);
        socket.init(transport);

        mClients.put(sid, socket);
        socket.once("close", args -> mClients.remove(sid));

        emit("connection", socket);
    }

    private static Parser parserFromQuery(Map<String, String> query) {
        return (query != null &&
                query.containsKey("EIO") &&
                query.get("EIO").equals("4"))? Parser.PROTOCOL_V4 : Parser.PROTOCOL_V3;
    }
}