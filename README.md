# Engine.IO Java
[![Build Status](https://travis-ci.org/socketio/engine.io-server-java.png?branch=master)](https://travis-ci.org/socketio/engine.io-server-java) [![codecov](https://codecov.io/gh/socketio/engine.io-server-java/branch/master/graph/badge.svg)](https://codecov.io/gh/socketio/engine.io-server-java)

This is the Engine.IO Server Library for Java ported from the [JavaScript server](https://github.com/socketio/engine.io).

See also: [Engine.IO-client Java](https://github.com/socketio/engine.io-client-java)

## Documentation
Complete documentation can be found [here](https://socketio.github.io/engine.io-server-java/).

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

    private final EngineIoServer mEngineIoServer = new EngineIoServer();

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        mEngineIoServer.handleRequest(request, response);
    }
}
```

Listen for new connections as follows:
```java
EngineIoServer server;  // server instance
server.on("connection", new Emitter.Listener() {
    @Override
    public void call(Object... args) {
        EngineIoSocket socket = (EngineIoSocket) args[0];
        // Do something with socket like store it somewhere
    }
});
```

Listen for raw packets received as follows:
```java
EngineIoSocket socket;  // socket received in "connection" event

socket.on("packet", new Emitter.Listener() {
    @Override
    public void call(Object... args) {
        Packet packet = (Packet) args[0];
        // Do something with packet.
    }
});
```

Listen for messages from the remote client as follows:
```java
EngineIoSocket socket;  // socket received in "connection" event

socket.on("message", new Emitter.Listener() {
    @Override
    public void call(Object... args) {
        Object message = args[0];
        // message can be either String or byte[]
        // Do something with message.
    }
});
```

Send a packet to client as follows:
```java
EngineIoSocket socket;  // socket received in "connection" event

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
