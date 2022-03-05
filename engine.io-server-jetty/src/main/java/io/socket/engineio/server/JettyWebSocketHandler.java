package io.socket.engineio.server;

import io.socket.engineio.server.utils.ParseQS;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * Adapter for Jetty WebSocket implementation.
 */
public final class JettyWebSocketHandler extends EngineIoWebSocket implements WebSocketListener {

    private final EngineIoServer mServer;

    private Session mSession;
    private Map<String, String> mQuery;
    private Map<String, List<String>> mHeaders;

    @SuppressWarnings("WeakerAccess")
    public JettyWebSocketHandler(EngineIoServer server) {
        mServer = server;
    }

    /* EngineIoWebSocket */

    @Override
    public Map<String, String> getQuery() {
        return mQuery;
    }

    @Override
    public Map<String, List<String>> getConnectionHeaders() {
        return mHeaders;
    }

    @Override
    public void write(String message) throws IOException {
        assert mSession != null;

        mSession.getRemote().sendString(message);
    }

    @Override
    public void write(byte[] message) throws IOException {
        assert mSession != null;

        mSession.getRemote().sendBytes(ByteBuffer.wrap(message));
    }

    @Override
    public void close() {
        if (mSession != null)
            mSession.close();
    }

    /* WebSocketListener */

    @Override
    public void onWebSocketConnect(Session session) {
        mSession = session;
        mQuery = ParseQS.decode(session.getUpgradeRequest().getQueryString());
        mHeaders = session.getUpgradeRequest().getHeaders();

        mServer.handleWebSocket(this);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        emit("close");
        mSession = null;
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        emit("error", "write error", cause.getMessage());
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        byte[] message;
        if((offset == 0) && (len == payload.length)) {
            message = payload;
        } else {
            message = new byte[len];
            System.arraycopy(payload, offset, message, 0, len);
        }

        emit("message", (Object) message);
    }

    @Override
    public void onWebSocketText(String message) {
        emit("message", (Object) message);
    }
}
