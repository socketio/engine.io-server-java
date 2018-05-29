package io.socket.engineio.server;

import io.socket.emitter.Emitter;
import io.socket.engineio.parser.Packet;
import org.eclipse.jetty.http.pathmap.ServletPathSpec;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * Test server against JS client.
 */
@SuppressWarnings("Duplicates")
public final class ServerTest {

    private static final AtomicInteger PORT_START = new AtomicInteger(3000);

    private static final class ServerWrapper {

        private int mPort;
        private Server mServer;
        private EngineIoServer mEngineIoServer;

        ServerWrapper() {
            mPort = PORT_START.getAndIncrement();
            mServer = new Server(mPort);
            mEngineIoServer = new EngineIoServer();

            ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
            servletContextHandler.setContextPath("/");

            servletContextHandler.addServlet(new ServletHolder(new HttpServlet() {
                @Override
                protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
                    mEngineIoServer.handleRequest(request, response);
                }
            }), "/engine.io/*");

            try {
                WebSocketUpgradeFilter webSocketUpgradeFilter = WebSocketUpgradeFilter.configureContext(servletContextHandler);
                webSocketUpgradeFilter.addMapping(new ServletPathSpec("/engine.io/*"), new WebSocketCreator() {
                    @Override
                    public Object createWebSocket(ServletUpgradeRequest servletUpgradeRequest, ServletUpgradeResponse servletUpgradeResponse) {
                        return new JettyWebSocketHandler(mEngineIoServer);
                    }
                });
            } catch (ServletException ex) {
                ex.printStackTrace();
            }

            HandlerList handlerList = new HandlerList();
            handlerList.setHandlers(new Handler[] { servletContextHandler });
            mServer.setHandler(handlerList);
        }

        void startServer() throws Exception {
            mServer.start();
        }

        void stopServer() throws Exception {
            mServer.stop();
        }

        int getPort() {
            return mPort;
        }

        EngineIoServer getEngineIoServer() {
            return mEngineIoServer;
        }
    }

    @Test
    public void connectTest() throws Exception {
        ServerWrapper serverWrapper = new ServerWrapper();
        try {
            serverWrapper.startServer();
            assertEquals(0, executeScriptForResult("src/test/resources/testServer_connect.js", serverWrapper.getPort()));
        } finally {
            serverWrapper.stopServer();
        }
    }

    @Test
    public void echoTest_string() throws Exception {
        final ServerWrapper serverWrapper = new ServerWrapper();
        try {
            serverWrapper.startServer();
            serverWrapper.getEngineIoServer().on("connection", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    final EngineIoSocket socket = (EngineIoSocket) args[0];
                    socket.on("message", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            Packet packet = new Packet(Packet.MESSAGE);
                            packet.data = args[0];
                            socket.send(packet);
                        }
                    });
                }
            });

            assertEquals(0, executeScriptForResult("src/test/resources/testServer_echo_string.js", serverWrapper.getPort()));
        } finally {
            serverWrapper.stopServer();
        }
    }

    @Test
    public void echoTest_binary() throws Exception {
        ServerWrapper serverWrapper = new ServerWrapper();
        try {
            serverWrapper.startServer();
            serverWrapper.getEngineIoServer().on("connection", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    final EngineIoSocket socket = (EngineIoSocket) args[0];
                    socket.on("message", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            Packet packet = new Packet(Packet.MESSAGE);
                            packet.data = args[0];
                            socket.send(packet);
                        }
                    });
                }
            });

            assertEquals(0, executeScriptForResult("src/test/resources/testServer_echo_binary.js", serverWrapper.getPort()));
        } finally {
            serverWrapper.stopServer();
        }
    }

    @Test
    public void echoTest_base64() throws Exception {
        ServerWrapper serverWrapper = new ServerWrapper();
        try {
            serverWrapper.startServer();
            serverWrapper.getEngineIoServer().on("connection", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    final EngineIoSocket socket = (EngineIoSocket) args[0];
                    socket.on("message", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            Packet packet = new Packet(Packet.MESSAGE);
                            packet.data = args[0];
                            socket.send(packet);
                        }
                    });
                }
            });

            assertEquals(0, executeScriptForResult("src/test/resources/testServer_echo_base64.js", serverWrapper.getPort()));
        } finally {
            serverWrapper.stopServer();
        }
    }

    @Test
    public void reverseEchoTest_string() throws Exception {
        final ServerWrapper serverWrapper = new ServerWrapper();
        try {
            serverWrapper.startServer();
            serverWrapper.getEngineIoServer().on("connection", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    final EngineIoSocket socket = (EngineIoSocket) args[0];
                    final String echoMessage = ServerTest.class.getSimpleName() + System.currentTimeMillis();
                    socket.on("message", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            assertEquals(echoMessage, args[0]);
                        }
                    });

                    Packet packet = new Packet(Packet.MESSAGE);
                    packet.data = echoMessage;
                    socket.send(packet);
                }
            });

            assertEquals(0, executeScriptForResult("src/test/resources/testServer_reverseEcho_string.js", serverWrapper.getPort()));
        } finally {
            serverWrapper.stopServer();
        }
    }

    private int executeScriptForResult(String script, int port) throws IOException {
        Process process = Runtime.getRuntime().exec("node " + script, new String[] {
                "PORT=" + port
        });
        try {
            int result = process.waitFor();

            InputStream inputStream = process.getInputStream();
            byte[] buffer = new byte[inputStream.available()];
            //noinspection ResultOfMethodCallIgnored
            inputStream.read(buffer);
            System.out.println(new String(buffer, StandardCharsets.UTF_8));

            return result;
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}