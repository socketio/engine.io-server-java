===============
Getting Started
===============

Create a servlet for handling incoming HTTP connections and call
``EngineIoServer`` class's ``handleRequest`` method on receiving an HTTP
request.

Example servlet
===============
Example servlet class::

    @WebServlet("/engine.io/*")
    public class EngineIoServlet extends HttpServlet {

        private final EngineIoServer mEngineIoServer = new EngineIoServer();

        @Override
        protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
            mEngineIoServer.handleRequest(request, response);
        }
    }

In the example servlet above, a static instance of ``EngineIoServer`` is defined and
the method ``service`` is overridden to call ``handleRequest``.

Listening for connections
=========================

Attach a listener to the ``connection`` event of ``EngineIoServer`` to listen for
new connections.

Example::

    server.on("connection", new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            EngineIoSocket socket = (EngineIoSocket) args[0];
            // Do something with socket
        }
    });

Listening for message from client
=================================

Attach a listener to the ``message`` event of ``EngineIoSocket`` to listen for
messages from client.

Example::

    socket.on("message", new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Object message = args[0];
            // message can be either String or byte[]
            // Do something with message.
        }
    });

Sending message to client
=========================

Call the ``send`` method on ``EngineIoSocket`` to send packet to remote client.

Example::

    socket.send(new Packet<>(Packet.MESSAGE, "foo"));

WebSocket connections
=====================

Handling WebSocket connections involves creating an instance of ``EngineIoWebSocket`` and
passing it in a call to ``handleWebSocket`` method of ``EngineIoServer``. The process to do
this is different for each server (*viz.* Tomcat, Jetty).

Generic server
--------------

Add an endpoint like the following::

    public final class EngineIoEndpoint extends Endpoint {

        private Session mSession;
        private Map<String, String> mQuery;
        private EngineIoWebSocket mEngineIoWebSocket;
        
        private EngineIoServer mEngineIoServer; // The engine.io server instance

        @Override
        public void onOpen(Session session, EndpointConfig endpointConfig) {
            mSession = session;
            mQuery = ParseQS.decode(session.getQueryString());

            mEngineIoWebSocket = new EngineIoWebSocketImpl();

            /*
             * These cannot be converted to lambda because of runtime type inference
             * by server.
             */
            mSession.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    mEngineIoWebSocket.emit("message", message);
                }
            });
            mSession.addMessageHandler(new MessageHandler.Whole<byte[]>() {
                @Override
                public void onMessage(byte[] message) {
                    mEngineIoWebSocket.emit("message", (Object)message);
                }
            });

            mEngineIoServer.handleWebSocket(mEngineIoWebSocket);
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            super.onClose(session, closeReason);

            mEngineIoWebSocket.emit("close");
            mSession = null;
        }

        @Override
        public void onError(Session session, Throwable thr) {
            super.onError(session, thr);

            mEngineIoWebSocket.emit("error", "unknown error", thr.getMessage());
        }
        
        private class EngineIoWebSocketImpl extends EngineIoWebSocket {

            private RemoteEndpoint.Basic mBasic;

            EngineIoWebSocketImpl() {
                mBasic = mSession.getBasicRemote();
            }

            @Override
            public Map<String, String> getQuery() {
                return mQuery;
            }

            @Override
            public void write(String message) throws IOException {
                mBasic.sendText(message);
            }

            @Override
            public void write(byte[] message) throws IOException {
                mBasic.sendBinary(ByteBuffer.wrap(message));
            }

            @Override
            public void close() {
                try {
                    mSession.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

The endpoint can be registered by annotation or a ``ServerApplicationConfig``
class like the following::

    public final class ApplicationServerConfig implements ServerApplicationConfig {

        @Override
        public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
            final HashSet<ServerEndpointConfig> result = new HashSet<>();
            result.add(ServerEndpointConfig.Builder
                    .create(EngineIoEndpoint.class, "/engine.io/")
                    .build());

            return result;
        }

        @Override
        public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
            return null;
        }
    }

Async Polling
=============

To enable async polling, mark the servlet with ``asyncSupported`` set to ``true``.
This can be done with annotations as ``@WebServlet(value = "/engine.io/*", asyncSupported = true)``.
Or with XML as ``<async-supported>true</async-supported>``.
Enabling async is recommended as the server is bombarded with GET requests if async is disabled.
