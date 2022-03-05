package io.socket.engineio.server.parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface Parser {

    Map<String, Integer> PACKETS = Collections.unmodifiableMap(new HashMap<String, Integer>() {{
        put(Packet.OPEN, 0);
        put(Packet.CLOSE, 1);
        put(Packet.PING, 2);
        put(Packet.PONG, 3);
        put(Packet.MESSAGE, 4);
        put(Packet.UPGRADE, 5);
        put(Packet.NOOP, 6);
    }});
    Map<Integer, String> PACKETS_REVERSE = Collections.unmodifiableMap(new HashMap<Integer, String>() {{
        put(0, Packet.OPEN);
        put(1, Packet.CLOSE);
        put(2, Packet.PING);
        put(3, Packet.PONG);
        put(4, Packet.MESSAGE);
        put(5, Packet.UPGRADE);
        put(6, Packet.NOOP);
    }});

    Packet<String> ERROR_PACKET = new Packet<>(Packet.ERROR, "parser error");

    Parser PROTOCOL_V3 = new ParserV3();
    Parser PROTOCOL_V4 = new ParserV4();

    interface EncodeCallback<T> {
        void call(T data);
    }

    interface DecodePayloadCallback<T> {
        boolean call(Packet<T> packet, int index, int total);
    }

    int getProtocolVersion();

    void encodePacket(Packet<?> packet, boolean supportsBinary, EncodeCallback<Object> callback);
    Packet<?> decodePacket(Object data);

    void encodePayload(List<Packet<?>> packets, boolean supportsBinary, EncodeCallback<Object> callback);
    void decodePayload(Object data, DecodePayloadCallback<Object> callback);
}
