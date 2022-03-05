package io.socket.engineio.server;

import io.socket.engineio.server.parser.Packet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("Duplicates")
public final class UpgradeTest {

    @Test
    public void echoTest_string() throws Exception {
        final ServerWrapper serverWrapper = new ServerWrapper();
        try {
            serverWrapper.startServer();
            serverWrapper.getEngineIoServer().on("connection", args -> {
                final EngineIoSocket socket = (EngineIoSocket) args[0];
                socket.on("message", args1 -> {
                    assertEquals("Upgraded Hello World", args1[0]);

                    Packet packet = new Packet(Packet.MESSAGE);
                    packet.data = args1[0];
                    socket.send(packet);
                });
            });

            assertEquals(0, TestUtils.executeScriptForResult("src/test/resources/testUpgrade_echo.js", serverWrapper.getPort()));
        } finally {
            serverWrapper.stopServer();
        }
    }
}