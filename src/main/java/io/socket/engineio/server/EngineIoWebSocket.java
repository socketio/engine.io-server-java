package io.socket.engineio.server;

import io.socket.emitter.Emitter;

import java.io.IOException;
import java.util.Map;

/**
 * Adapter between different WebSocket implementations and Engine.IO
 */
public abstract class EngineIoWebSocket extends Emitter {

    public abstract Map<String, String> getQuery();
    public abstract void write(String message) throws IOException;
    public abstract void write(byte[] message) throws IOException;
    public abstract void close();
}