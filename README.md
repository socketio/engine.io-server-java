# Engine.IO Java
[![Build Status](https://travis-ci.org/socketio/engine.io-server-java.png?branch=master)](https://travis-ci.org/socketio/engine.io-server-java) [![codecov](https://codecov.io/gh/socketio/engine.io-server-java/branch/master/graph/badge.svg)](https://codecov.io/gh/socketio/engine.io-server-java)

This is the Engine.IO Server Library for Java ported from the [JavaScript server](https://github.com/socketio/engine.io).

See also: [Engine.IO-client Java](https://github.com/socketio/engine.io-client-java)

## Installation
This section will be updated when artifact is available on Maven Central.

### Maven
This section will be updated when artifact is available on Maven Central.

### Gradle
This section will be updated when artifact is available on Maven Central.

#### Engine.IO Protocol 1.x suppport

The current version of engine.io-java does not support protocol 1.x.

## Usage
Usage is slightly different based on the server being used.

Create a servlet to handle the HTTP requests as follows:
```java
public class EngineIoServlet extends HttpServlet {

    private static final EngineIoServer ENGINE_IO_SERVER = new EngineIoServer();

    public static EngineIoServer getEngineIoServer() {
        return ENGINE_IO_SERVER;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ENGINE_IO_SERVER.handleRequest(request, response);
    }
}
```

Listen for new connections as follows:
```java
EngineIoServer server = EngineIoServlet.getEngineIoServer();
server.on("connection", new Emitter.Listener() {
    @Override
    public void call(Object... args) {
        EngineIoSocket socket = (EngineIoSocket) args[0];
        // Do something with socket
    }
});
```

Listen for packets as follows:
```java
EngineIoSocket socket;

socket.on("packet", new Emitter.Listener() {
    @Override
    public void call(Object... args) {
        Packet packet = (Packet) args[0];
        // Do something with packet.
    }
});
```

Listen for message as follows:
```java
EngineIoSocket socket;

socket.on("message", new Emitter.Listener() {
    @Override
    public void call(Object... args) {
        Object message = args[0];
        // Do something with message.
    }
});
```

Send a packet to client as follows:
```java
EngineIoSocket socket;

socket.send(new Packet<>(Packet.MESSAGE, "foo"));
```

### Jetty specific
To handle WebSocket connections in jetty, add the `engine.io-jetty` module.
Then, add the `JettyWebSocketHandler` to the servlet context as follows:
```java
ServletContextHandler servletContextHandler;

WebSocketUpgradeFilter webSocketUpgradeFilter = WebSocketUpgradeFilter.configureContext(servletContextHandler);
webSocketUpgradeFilter.addMapping(new ServletPathSpec("/engine.io/*"), new WebSocketCreator() {
    @Override
    public Object createWebSocket(ServletUpgradeRequest servletUpgradeRequest, ServletUpgradeResponse servletUpgradeResponse) {
        return new JettyWebSocketHandler(EngineIoServlet.getEngineIoServer());
    }
});
```

## Features
This library supports all of the features the JS server does, including events, options and upgrading transport.

## License

MIT
