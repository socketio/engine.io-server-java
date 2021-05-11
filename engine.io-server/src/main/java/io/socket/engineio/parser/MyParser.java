package io.socket.engineio.parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface MyParser {

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

    static byte[] concatBuffer(byte[] ...arrays) {
        int length = 0;
        for (byte[] item : arrays) {
            length += item.length;
        }

        final byte[] result = new byte[length];
        int idx = 0;
        for (byte[] item : arrays) {
            System.arraycopy(item, 0, result, idx, item.length);
            idx += item.length;
        }

        return result;
    }
}
