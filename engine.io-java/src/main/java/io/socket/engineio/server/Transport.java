package io.socket.engineio.server;

import io.socket.emitter.Emitter;
import io.socket.engineio.parser.Packet;
import io.socket.engineio.parser.ServerParser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public abstract class Transport extends Emitter {

    @SuppressWarnings("WeakerAccess")
    protected ReadyState mReadyState;

    protected Transport() {
        mReadyState = ReadyState.OPEN;
    }

    public abstract void onRequest(HttpServletRequest request, HttpServletResponse response) throws IOException;
    public abstract void send(List<Packet> packets);
    public abstract boolean isWritable();
    public abstract String getName();

    protected abstract void doClose();

    public void close() {
        if(mReadyState != ReadyState.CLOSED && mReadyState != ReadyState.CLOSING) {
            mReadyState = ReadyState.CLOSING;
            doClose();
        }
    }

    protected void onError(String reason, String description) {
        if(this.listeners("error").size() > 0) {
            emit("error", reason, description);
        }
    }

    protected void onPacket(Packet packet) {
        emit("packet", packet);
    }

    protected void onData(Object data) {
        onPacket(ServerParser.decodePacket(data));
    }

    protected void onClose() {
        mReadyState = ReadyState.CLOSED;
        emit("close");
    }
}