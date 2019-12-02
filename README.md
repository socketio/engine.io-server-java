# Engine.IO Java
[![Build Status](https://travis-ci.org/socketio/engine.io-server-java.png?branch=master)](https://travis-ci.org/socketio/engine.io-server-java) [![codecov](https://codecov.io/gh/socketio/engine.io-server-java/branch/master/graph/badge.svg)](https://codecov.io/gh/socketio/engine.io-server-java)

This is the Engine.IO Server Library for Java ported from the [JavaScript server](https://github.com/socketio/engine.io).

See also: [Engine.IO-client Java](https://github.com/socketio/engine.io-client-java)

## Documentation
Complete documentation can be found [here](https://socketio.github.io/engine.io-server-java/).

## Installation
The latest artifact is available on Maven Central.

### Maven
Add the following dependency to your `pom.xml`.

```xml
<dependencies>
  <dependency>
    <groupId>io.socket</groupId>
    <artifactId>engine.io-server</artifactId>
    <version>1.3.2</version>
  </dependency>
</dependencies>
```

### Gradle
Add it as a gradle dependency in `build.gradle`.

```groovy
compile ('io.socket:engine.io-server:1.3.2')
```

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

### WebSockets
Please see the complete documentation on handling WebSockets [here](https://socketio.github.io/engine.io-server-java/using.html#websocket-connections).

## Features
This library supports all of the features the JS server does, including events, options and upgrading transport.

## License

MIT
