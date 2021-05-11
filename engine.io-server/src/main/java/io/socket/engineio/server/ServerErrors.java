package io.socket.engineio.server;

enum ServerErrors {
    UNKNOWN_TRANSPORT(0, "Transport unknown"),
    UNKNOWN_SID(1, "Session ID unknown"),
    BAD_HANDSHAKE_METHOD(2, "Bad handshake method"),
    BAD_REQUEST(3, "Bad request"),
    FORBIDDEN(4, "Forbidden"),
    UNSUPPORTED_PROTOCOL_VERSION(5, "Unsupported protocol version");

    private final int mCode;
    private final String mMessage;

    ServerErrors(int code, String message) {
        mCode = code;
        mMessage = message;
    }

    public int getCode() {
        return mCode;
    }

    public String getMessage() {
        return mMessage;
    }
}