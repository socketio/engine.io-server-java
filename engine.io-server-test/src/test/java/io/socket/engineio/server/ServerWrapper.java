package io.socket.engineio.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

final class ServerWrapper {

    private static final AtomicInteger PORT_START = new AtomicInteger(3000);

    private final int mPort;
    private final Server mServer;
    private final EngineIoServer mEngineIoServer;

    static {
        Log.setLog(new JettyNoLogging());
    }

    ServerWrapper() {
        mPort = PORT_START.getAndIncrement();
        mServer = new Server(mPort);
        mEngineIoServer = new EngineIoServer();

        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContextHandler.setContextPath("/");

        /*final ServletHolder servletHolder = new ServletHolder(new HttpServlet() {
            private static final long serialVersionUID = 7089396507926086181L;

            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
                mEngineIoServer.handleRequest(request, response);
            }
        });
        servletHolder.setAsyncSupported(true);
        servletContextHandler.addServlet(servletHolder, "/engine.io/*");*/

        servletContextHandler.addServlet(new ServletHolder(new HttpServlet() {
            private static final long serialVersionUID = 6467153647501924502L;

            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
                final String path = request.getPathInfo();
                final File file = new File("src/test/resources", path);
                if (file.exists()) {
                    response.setContentType(Files.probeContentType(file.toPath()));

                    try (FileInputStream fis = new FileInputStream(file)) {
                        IOUtils.copy(fis, response.getOutputStream());
                    }
                } else {
                    response.setStatus(404);
                    try (PrintWriter writer = response.getWriter()) {
                        writer.print("Not Found");
                        writer.flush();
                    }
                }
            }
        }), "/*");

        final JettyWebSocketServlet webSocketServlet = new JettyWebSocketServlet() {
            private static final long serialVersionUID = 4525525859144703715L;

            @Override
            protected void configure(JettyWebSocketServletFactory jettyWebSocketServletFactory) {
                jettyWebSocketServletFactory.addMapping(
                        "/",
                        (request, response) -> new JettyEngineIoWebSocketHandler(mEngineIoServer));
            }

            @Override
            public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
                if (request instanceof HttpServletRequest) {
                    final String upgradeHeader = ((HttpServletRequest) request).getHeader("upgrade");
                    if (upgradeHeader != null) {
                        super.service(request, response);
                    } else {
                        mEngineIoServer.handleRequest((HttpServletRequest) request, (HttpServletResponse) response);
                    }
                } else {
                    super.service(request, response);
                }
            }
        };
        final ServletHolder webSocketServletHolder = new ServletHolder(webSocketServlet);
        webSocketServletHolder.setAsyncSupported(true);
        servletContextHandler.addServlet(webSocketServletHolder, "/engine.io/*");
        JettyWebSocketServletContainerInitializer.configure(servletContextHandler, null);

        mServer.setHandler(servletContextHandler);
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

    private static final class JettyNoLogging implements Logger {

        @Override public String getName() {
            return "no";
        }

        @Override
        public void warn(String s, Object... objects) {
        }

        @Override
        public void warn(Throwable throwable) {
        }

        @Override
        public void warn(String s, Throwable throwable) {
        }

        @Override
        public void info(String s, Object... objects) {
        }

        @Override
        public void info(Throwable throwable) {
        }

        @Override
        public void info(String s, Throwable throwable) {
        }

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public void setDebugEnabled(boolean b) {
        }

        @Override
        public void debug(String s, Object... objects) {
        }

        @Override
        public void debug(String s, long l) {
        }

        @Override
        public void debug(Throwable throwable) {
        }

        @Override
        public void debug(String s, Throwable throwable) {
        }

        @Override
        public Logger getLogger(String s) {
            return this;
        }

        @Override
        public void ignore(Throwable throwable) {
        }
    }
}
