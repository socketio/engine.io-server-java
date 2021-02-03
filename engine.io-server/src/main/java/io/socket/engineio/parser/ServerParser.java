package io.socket.engineio.parser;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Handles parsing of engine.io packets and payloads.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class ServerParser {

    @SuppressWarnings("WeakerAccess")
    public static final int PROTOCOL = Parser.PROTOCOL;
    private static final String SEPARATOR = "\u001E";   // (char) 30
    private static final Packet<String> ERROR_PACKET = new Packet<>(Packet.ERROR, "parser error");

    private static final Map<String, Integer> packets = new HashMap<String, Integer>() {{
        put(Packet.OPEN, 0);
        put(Packet.CLOSE, 1);
        put(Packet.PING, 2);
        put(Packet.PONG, 3);
        put(Packet.MESSAGE, 4);
        put(Packet.UPGRADE, 5);
        put(Packet.NOOP, 6);
    }};

    private static final Map<Integer, String> packetsList = new HashMap<>();
    static {
        for (Map.Entry<String, Integer> entry : packets.entrySet()) {
            packetsList.put(entry.getValue(), entry.getKey());
        }
    }

    private ServerParser() {}

    /**
     * Encode a packet for transfer over transport.
     *
     * @param packet The packet to encode.
     * @param supportsBinary Whether the transport supports binary encoding.
     * @param callback The callback to be called with the encoded data.
     */
    public static void encodePacket(Packet packet, boolean supportsBinary, Parser.EncodeCallback callback) {
        if (packet.data instanceof byte[]) {
            encodeByteArray(packet, supportsBinary, callback);
        } else {
            String encoded = String.valueOf(packets.get(packet.type));

            if (null != packet.data) {
                encoded += String.valueOf(packet.data);
            }

            ((Parser.EncodeCallback<String>) callback).call(encoded);
        }
    }

    /**
     * Encode an array of packets into a payload for transfer over transport.
     *
     * @param packets Array of packets to encode.
     * @param callback The callback to be called with the encoded data.
     */
    public static void encodePayload(List<Packet<?>> packets, Parser.EncodeCallback callback) {
        final String[] encodedPackets = new String[packets.size()];
        for (int i = 0; i < encodedPackets.length; i++) {
            final Packet<?> packet = packets.get(i);

            final int packetIdx = i;
            encodePacket(packet, false, data -> encodedPackets[packetIdx] = (String) data);
        }

        callback.call(String.join(SEPARATOR, encodedPackets));
    }

    /**
     * Decode a packet received from transport.
     *
     * @param data Data received from transport.
     * @return Packet decoded from data.
     */
    public static Packet decodePacket(Object data) {
        if(data == null) {
            return ERROR_PACKET;
        }

        if(data instanceof String) {
            final String stringData = (String) data;
            if(stringData.charAt(0) == 'b') {
                Packet<byte[]> packet = new Packet<>(Packet.MESSAGE);
                packet.data = java.util.Base64.getDecoder().decode(stringData.substring(1));
                return packet;
            } else {
                Packet<String> packet = new Packet<>(packetsList.get(
                        Integer.parseInt(String.valueOf(stringData.charAt(0)))));
                packet.data = stringData.substring(1);
                return packet;
            }
        } else if(data instanceof byte[]) {
            return new Packet<>(Packet.MESSAGE, (byte[]) data);
        } else {
            throw new IllegalArgumentException("Invalid type for data: " + data.getClass().getSimpleName());
        }
    }

    /**
     * Decode payload received from transport.
     *
     * @param data Data received from transport.
     * @param callback The callback to be called with each decoded packet in payload.
     */
    public static void decodePayload(Object data, Parser.DecodePayloadCallback callback) {
        assert callback != null;

        final ArrayList<Packet> packets = new ArrayList<>();
        if(data instanceof String) {
            final String[] encodedPackets = ((String) data).split(SEPARATOR);
            for (String encodedPacket : encodedPackets) {
                final Packet<?> packet = decodePacket(encodedPacket);
                packets.add(packet);

                if (packet.type.equals("error")) {
                    break;
                }
            }
        } else {
            throw new IllegalArgumentException("data must be a String");
        }

        for (int i = 0; i < packets.size(); i++) {
            if(!callback.call(packets.get(i), i, packets.size())) {
                return;
            }
        }
    }

    private static void encodeByteArray(Packet<byte[]> packet, boolean supportsBinary, Parser.EncodeCallback callback) {
        if (supportsBinary) {
            callback.call(packet.data);
        } else {
            String resultBuilder = "b" +
                    java.util.Base64.getEncoder().encodeToString(packet.data);
            callback.call(resultBuilder);
        }
    }
}