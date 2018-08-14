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
passing it to a call to ``handleWebSocket`` method of ``EngineIoServer``. The process to do
this is different for each server (*viz.* Tomcat, Jetty).

Jetty server
------------

For Jetty server, add the :ref:`install-jetty-ws-adapter` dependency.
Then add the following code can be used to listen for WebSocket connections::

    ServletContextHandler servletContextHandler;    // The jetty servlet context handler

    WebSocketUpgradeFilter webSocketUpgradeFilter = WebSocketUpgradeFilter.configureContext(servletContextHandler);
    webSocketUpgradeFilter.addMapping(new ServletPathSpec("/engine.io/*"), new WebSocketCreator() {
        @Override
        public Object createWebSocket(ServletUpgradeRequest servletUpgradeRequest, ServletUpgradeResponse servletUpgradeResponse) {
            return new JettyWebSocketHandler(EngineIoServlet.getEngineIoServer());
        }
    });
