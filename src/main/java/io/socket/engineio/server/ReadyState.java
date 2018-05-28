package io.socket.engineio.server;

enum ReadyState {
    OPENING, OPEN, CLOSING, CLOSED;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}