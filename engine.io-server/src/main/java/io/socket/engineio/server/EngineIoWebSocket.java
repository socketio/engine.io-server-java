package io.socket.engineio.server;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Adapter between different WebSocket implementations and Engine.IO.
 */
public abstract class EngineIoWebSocket extends Emitter {

    /**
     * Get the query of the initial HTTP request.
     *
     * @return Query parameters of the initial HTTP request.
     */
    public abstract Map<String, String> getQuery();

    /**
     * Get the headers in the initial HTTP request.
     *
     * @return Headers of the initial HTTP request.
     */
    public abstract Map<String, List<String>> getConnectionHeaders();

    /**
     * Write a string to the WebSocket and send to remote client.
     *
     * @param message String payload to send.
     * @throws IOException On write error.
     */
    public abstract void write(String message) throws IOException;

    /**
     * Write a byte array to the WebSocket and send to remote client.
     *
     * @param message Binary payload to send.
     * @throws IOException On write error.
     */
    public abstract void write(byte[] message) throws IOException;

    /**
     * Close the WebSocket.
     */
    public abstract void close();
}