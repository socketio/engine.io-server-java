package io.socket.engineio.server;

import io.socket.engineio.parser.Packet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test server against JS client.
 */
@SuppressWarnings("Duplicates")
public final class WebSocketTest {

    @Test
    public void connectTest() throws Exception {
        ServerWrapper serverWrapper = new ServerWrapper();
        try {
            serverWrapper.startServer();
            assertEquals(0, TestUtils.executeScriptForResult("src/test/resources/testWebSocket_connect.js", serverWrapper.getPort()));
        } finally {
            serverWrapper.stopServer();
        }
    }

    @Test
    public void echoTest_string() throws Exception {
        final ServerWrapper serverWrapper = new ServerWrapper();
        try {
            serverWrapper.startServer();
            serverWrapper.getEngineIoServer().on("connection", args -> {
                final EngineIoSocket socket = (EngineIoSocket) args[0];
                socket.on("message", args1 -> {
                    Packet packet = new Packet(Packet.MESSAGE);
                    packet.data = args1[0];
                    socket.send(packet);
                });
            });

            assertEquals(0, TestUtils.executeScriptForResult("src/test/resources/testWebSocket_echo_string.js", serverWrapper.getPort()));
        } finally {
            serverWrapper.stopServer();
        }
    }

    @Test
    public void echoTest_binary() throws Exception {
        ServerWrapper serverWrapper = new ServerWrapper();
        try {
            serverWrapper.startServer();
            serverWrapper.getEngineIoServer().on("connection", args -> {
                final EngineIoSocket socket = (EngineIoSocket) args[0];
                socket.on("message", args1 -> {
                    Packet packet = new Packet(Packet.MESSAGE);
                    packet.data = args1[0];
                    socket.send(packet);
                });
            });

            assertEquals(0, TestUtils.executeScriptForResult("src/test/resources/testWebSocket_echo_binary.js", serverWrapper.getPort()));
        } finally {
            serverWrapper.stopServer();
        }
    }

    @Test
    public void echoTest_base64() throws Exception {
        ServerWrapper serverWrapper = new ServerWrapper();
        try {
            serverWrapper.startServer();
            serverWrapper.getEngineIoServer().on("connection", args -> {
                final EngineIoSocket socket = (EngineIoSocket) args[0];
                socket.on("message", args1 -> {
                    Packet packet = new Packet(Packet.MESSAGE);
                    packet.data = args1[0];
                    socket.send(packet);
                });
            });

            assertEquals(0, TestUtils.executeScriptForResult("src/test/resources/testWebSocket_echo_base64.js", serverWrapper.getPort()));
        } finally {
            serverWrapper.stopServer();
        }
    }

    @Test
    public void reverseEchoTest() throws Exception {
        final ServerWrapper serverWrapper = new ServerWrapper();
        try {
            serverWrapper.startServer();
            serverWrapper.getEngineIoServer().on("connection", args -> {
                final EngineIoSocket socket = (EngineIoSocket) args[0];
                final String echoMessage = PollingTest.class.getSimpleName() + System.currentTimeMillis();
                socket.on("message", args1 -> assertEquals(echoMessage, args1[0]));

                Packet packet = new Packet(Packet.MESSAGE);
                packet.data = echoMessage;
                socket.send(packet);
            });

            assertEquals(0, TestUtils.executeScriptForResult("src/test/resources/testWebSocket_reverseEcho.js", serverWrapper.getPort()));
        } finally {
            serverWrapper.stopServer();
        }
    }
}