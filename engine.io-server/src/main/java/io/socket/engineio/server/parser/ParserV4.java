package io.socket.engineio.server.parser;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@SuppressWarnings("unchecked")
public final class ParserV4 implements Parser {

    public static final int PROTOCOL = 4;
    private static final String SEPARATOR = "\u001E";   // (char) 30

    @Override
    public int getProtocolVersion() {
        return PROTOCOL;
    }

    /**
     * Encode a packet for transfer over transport.
     *
     * @param packet The packet to encode.
     * @param supportsBinary Whether the transport supports binary encoding.
     * @param callback The callback to be called with the encoded data.
     */
    @Override
    public void encodePacket(Packet<?> packet, boolean supportsBinary, EncodeCallback<Object> callback) {
        if (packet.data instanceof byte[]) {
            encodeByteArray((Packet<byte[]>) packet, supportsBinary, callback);
        } else {
            String encoded = String.valueOf(PACKETS.get(packet.type));

            if (null != packet.data) {
                encoded += String.valueOf(packet.data);
            }

            callback.call(encoded);
        }
    }

    public static void encodeByteArray(Packet<byte[]> packet, boolean supportsBinary, EncodeCallback<Object> callback) {
        if (supportsBinary) {
            callback.call(packet.data);
        } else {
            final String resultBuilder = "b" +
                    Base64.getEncoder().encodeToString(packet.data);
            callback.call(resultBuilder);
        }
    }

    /**
     * Encode an array of packets into a payload for transfer over transport.
     *
     * @param packets Array of packets to encode.
     * @param callback The callback to be called with the encoded data.
     */
    @Override
    public void encodePayload(List<Packet<?>> packets, boolean supportsBinary, EncodeCallback<Object> callback) {
        final String[] encodedPackets = new String[packets.size()];
        for (int i = 0; i < encodedPackets.length; i++) {
            final Packet<?> packet = packets.get(i);

            final int packetIdx = i;
            encodePacket(packet, false, data -> encodedPackets[packetIdx] = (String) data);
        }

        callback.call(String.join(SEPARATOR, encodedPackets));
    }

    /**
     * Decode payload received from transport.
     *
     * @param data Data received from transport.
     * @param callback The callback to be called with each decoded packet in payload.
     */
    @Override
    public void decodePayload(Object data, DecodePayloadCallback<Object> callback) {
        assert callback != null;

        final ArrayList<Packet<?>> packets = new ArrayList<>();
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
            if(!callback.call((Packet<Object>) packets.get(i), i, packets.size())) {
                return;
            }
        }
    }

    /**
     * Decode a packet received from transport.
     *
     * @param data Data received from transport.
     * @return Packet decoded from data.
     */
    @Override
    public Packet<?> decodePacket(Object data) {
        if(data == null) {
            return ERROR_PACKET;
        }

        if(data instanceof String) {
            final String stringData = (String) data;
            if(stringData.charAt(0) == 'b') {
                final Packet<byte[]> packet = new Packet<>(Packet.MESSAGE);
                packet.data = java.util.Base64.getDecoder().decode(stringData.substring(1));
                return packet;
            } else {
                final Packet<String> packet = new Packet<>(PACKETS_REVERSE.get(
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
}
