package io.socket.engineio.server.transport;

import io.socket.engineio.parser.Packet;
import io.socket.engineio.parser.Parser;
import io.socket.engineio.parser.ServerParser;
import io.socket.engineio.server.EngineIoWebSocket;
import io.socket.engineio.server.Transport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * WebSocket transport.
 */
public final class WebSocket extends Transport {

    public static final String NAME = "websocket";

    private final EngineIoWebSocket mConnection;

    public WebSocket(EngineIoWebSocket webSocket) {
        mConnection = webSocket;
        mConnection.on("message", args -> onData(args[0]));
        mConnection.on("close", args -> onClose());
        mConnection.on("error", args -> onError((String) args[0], (String) args[1]));
    }

    @Override
    public void onRequest(HttpServletRequest request, HttpServletResponse response) { }

    @Override
    public void send(List<Packet<?>> packets) {
        final Parser.EncodeCallback<?> encodeCallback = data -> {
            try {
                if(data instanceof String) {
                    mConnection.write((String) data);
                } else if(data instanceof byte[]) {
                    mConnection.write((byte[]) data);
                }
            } catch (IOException ex) {
                onError("write error", ex.getMessage());
            }
        };
        for (Packet<?> packet : packets) {
            ServerParser.encodePacket(packet, true, encodeCallback);
        }
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected void doClose() {
        mConnection.close();
    }
}