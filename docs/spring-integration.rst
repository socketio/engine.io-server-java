==================
Spring Integration
==================

This guide describes how to configure engine.io with Spring Framework and Spring Boot.

Add a class to contain the bulk of engine.io handling code::

    import io.socket.engineio.server.EngineIoServer;
    import io.socket.engineio.server.EngineIoWebSocket;
    import io.socket.engineio.server.utils.ParseQS;
    import org.springframework.http.server.ServerHttpRequest;
    import org.springframework.http.server.ServerHttpResponse;
    import org.springframework.stereotype.Controller;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RequestMethod;
    import org.springframework.web.socket.*;
    import org.springframework.web.socket.server.HandshakeInterceptor;

    import javax.servlet.http.HttpServletRequest;
    import javax.servlet.http.HttpServletResponse;
    import java.io.IOException;
    import java.util.HashMap;
    import java.util.Map;

    @Controller
    public final class EngineIoHandler implements HandshakeInterceptor, WebSocketHandler {

        private static final String ATTRIBUTE_ENGINEIO_BRIDGE = "engineIo.bridge";
        private static final String ATTRIBUTE_ENGINEIO_QUERY = "engineIo.query";

        private final EngineIoServer mEngineIoServer;

        public EngineIoHandler(EngineIoServer engineIoServer) {
            mEngineIoServer = engineIoServer;
        }

        @RequestMapping(
                value = "/engine.io/",
                method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS},
                headers = "Connection!=Upgrade")
        public void httpHandler(HttpServletRequest request, HttpServletResponse response) throws IOException {
            mEngineIoServer.handleRequest(request, response);
        }

        /* HandshakeInterceptor */

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
            attributes.put(ATTRIBUTE_ENGINEIO_QUERY, request.getURI().getQuery());
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        }

        /* WebSocketHandler */

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession webSocketSession) {
            final EngineIoSpringWebSocket webSocket = new EngineIoSpringWebSocket(webSocketSession);
            webSocketSession.getAttributes().put(ATTRIBUTE_ENGINEIO_BRIDGE, webSocket);
            mEngineIoServer.handleWebSocket(webSocket);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus closeStatus) {
            ((EngineIoSpringWebSocket)webSocketSession.getAttributes().get(ATTRIBUTE_ENGINEIO_BRIDGE))
                    .afterConnectionClosed(closeStatus);
        }

        @Override
        public void handleMessage(WebSocketSession webSocketSession, WebSocketMessage<?> webSocketMessage) {
            ((EngineIoSpringWebSocket)webSocketSession.getAttributes().get(ATTRIBUTE_ENGINEIO_BRIDGE))
                    .handleMessage(webSocketMessage);
        }

        @Override
        public void handleTransportError(WebSocketSession webSocketSession, Throwable throwable) {
            ((EngineIoSpringWebSocket)webSocketSession.getAttributes().get(ATTRIBUTE_ENGINEIO_BRIDGE))
                    .handleTransportError(throwable);
        }

        private static final class EngineIoSpringWebSocket extends EngineIoWebSocket {

            private final WebSocketSession mSession;
            private final Map<String, String> mQuery;

            EngineIoSpringWebSocket(WebSocketSession session) {
                mSession = session;

                final String queryString = (String)mSession.getAttributes().get(ATTRIBUTE_ENGINEIO_QUERY);
                if (queryString != null) {
                    mQuery = ParseQS.decode(queryString);
                } else {
                    mQuery = new HashMap<>();
                }
            }

            /* EngineIoWebSocket */

            @Override
            public Map<String, String> getQuery() {
                return mQuery;
            }

            @Override
            public void write(String message) throws IOException {
                mSession.sendMessage(new TextMessage(message));
            }

            @Override
            public void write(byte[] message) throws IOException {
                mSession.sendMessage(new BinaryMessage(message));
            }

            @Override
            public void close() {
                try {
                    mSession.close();
                } catch (IOException ignore) {
                }
            }

            /* WebSocketHandler */

            void afterConnectionClosed(CloseStatus closeStatus) {
                emit("close");
            }

            void handleMessage(WebSocketMessage<?> message) {
                if (message.getPayload() instanceof String || message.getPayload() instanceof byte[]) {
                    emit("message", (Object) message.getPayload());
                } else {
                    throw new RuntimeException(String.format(
                            "Invalid message type received: %s. Expected String or byte[].",
                            message.getPayload().getClass().getName()));
                }
            }

            void handleTransportError(Throwable exception) {
                emit("error", "write error", exception.getMessage());
            }
        }
    }

**NOTE** If this class is discovered/instantiated multiple times, it may cause problems so place it accordingly.

Next, add bean declarations for the engine.io server::

    <bean id="engineIoServer" class="io.socket.engineio.server.EngineIoServer" />

Next, add websocket handling for engine.io server in XML::

    <websocket:handlers>
        <websocket:mapping path="/engine.io/" handler="engineIoHandler" />

        <websocket:handshake-interceptors>
            <beans:ref bean="engineIoHandler" />
        </websocket:handshake-interceptors>
    </websocket:handlers>

Or in Java::

    @Configuration
    @EnableWebSocket
    public class EngineIoConfigurator implements WebSocketConfigurer {
    
        private final EngineIoHandler mEngineIoHandler;
    
        public EngineIoConfigurator(EngineIoHandler engineIoHandler) {
            mEngineIoHandler = engineIoHandler;
        }
    
        @Override
        public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
            registry.addHandler(mEngineIoHandler, "/engine.io/")
                    .addInterceptors(mEngineIoHandler);
        }
    }

This serves as a gateway for engine.io and is same for all other server that builds on top of engine.io, viz. Socket.io .