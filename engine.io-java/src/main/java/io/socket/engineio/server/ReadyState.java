package io.socket.engineio.server;

/**
 * The possible states a connection can be in at any given time.
 * These states also apply to transports.
 */
public enum ReadyState {
    /**
     * The connection is opening. No IO operation can be performed.
     */
    OPENING,
    /**
     * The connection is open. IO operations can be performed on it.
     */
    OPEN,
    /**
     * The connection is closing. No IO operation can be performed.
     */
    CLOSING,
    /**
     * The connection is closed. No IO operation can be performed.
     */
    CLOSED
}