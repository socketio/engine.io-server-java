package io.socket.engineio.server;

import io.socket.emitter.Emitter;
import io.socket.engineio.parser.Packet;
import io.socket.engineio.parser.ServerParser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Base class for all transports.
 */
public abstract class Transport extends Emitter {

    @SuppressWarnings("WeakerAccess")
    protected ReadyState mReadyState;

    protected Transport() {
        mReadyState = ReadyState.OPEN;
    }

    /**
     * Handle a client HTTP request.
     *
     * @param request The HTTP request object.
     * @param response The HTTP response object.
     * @throws IOException On IO error.
     */
    public abstract void onRequest(HttpServletRequest request, HttpServletResponse response) throws IOException;

    /**
     * Send a list of packets over the transport.
     *
     * @param packets List of packets to send.
     */
    public abstract void send(List<Packet<?>> packets);

    /**
     * Checks whether the transport is currently writable.
     *
     * @return Boolean value indicating if transport can be written to.
     */
    public abstract boolean isWritable();

    /**
     * Get the name of this transport.
     *
     * @return Name of transport.
     */
    public abstract String getName();

    /**
     * Transport specific logic for closing transport.
     */
    protected abstract void doClose();

    /**
     * Close the transport if not already closed.
     */
    public void close() {
        if(mReadyState != ReadyState.CLOSED && mReadyState != ReadyState.CLOSING) {
            mReadyState = ReadyState.CLOSING;
            doClose();
        }
    }

    /**
     * Called by child class to indicate error with transport.
     *
     * @param reason Reason of error.
     * @param description Description of error.
     */
    protected void onError(String reason, String description) {
        if(this.listeners("error").size() > 0) {
            emit("error", reason, description);
        }
    }

    /**
     * Called by child to indicate a packet receive from remote client.
     *
     * @param packet Packet received by transport.
     */
    protected void onPacket(Packet<?> packet) {
        emit("packet", packet);
    }

    /**
     * Called by child to indicate data received from remote client.
     *
     * @param data Encoded data received by transport.
     */
    protected void onData(Object data) {
        onPacket(ServerParser.decodePacket(data));
    }

    /**
     * Called by child to indicate closure of transport.
     */
    protected void onClose() {
        mReadyState = ReadyState.CLOSED;
        emit("close");
    }
}