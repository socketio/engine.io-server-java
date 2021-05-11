package io.socket.engineio.parser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@SuppressWarnings("unchecked")
public final class ParserV3 implements MyParser {

    public static final int PROTOCOL = 3;

    /**
     * Encode a packet for transfer over transport.
     *
     * @param packet The packet to encode.
     * @param supportsBinary Whether the transport supports binary encoding.
     * @param callback The callback to be called with the encoded data.
     */
    public static void encodePacket(Packet<?> packet, boolean supportsBinary, Parser.EncodeCallback<Object> callback) {
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

    /**
     * Encode an array of packets into a payload for transfer over transport.
     *
     * @param packets Array of packets to encode.
     * @param supportsBinary Whether the transport supports binary encoding.
     * @param callback The callback to be called with the encoded data.
     */
    public static void encodePayload(List<Packet<?>> packets, boolean supportsBinary, Parser.EncodeCallback<Object> callback) {
        boolean isBinary = false;
        for (Packet<?> packet : packets) {
            if (packet.data instanceof byte[]) {
                isBinary = true;
                break;
            }
        }

        if (isBinary && supportsBinary) {
            encodePayloadAsBinary(packets, callback);
            return;
        }

        if (packets.size() == 0) {
            callback.call("0:");
            return;
        }

        final StringBuilder result = new StringBuilder();

        for (Packet<?> packet : packets) {
            encodePacket(packet, false, data -> result.append(setLengthHeader((String) data)));
        }

        callback.call(result.toString());
    }

    /**
     * Encode an array of packets into a binary payload for transfer over transport.
     *  @param packets Array of packets to encode.
     * @param callback The callback to be called with the encoded data.
     */
    @SuppressWarnings("Duplicates")
    public static void encodePayloadAsBinary(List<Packet<?>> packets, Parser.EncodeCallback<Object> callback) {
        if (packets.size() == 0) {
            callback.call(new byte[0]);
            return;
        }

        final ArrayList<byte[]> results = new ArrayList<>(packets.size());

        for (Packet<?> packet : packets) {
            encodePacket(packet, true, encodedPacket -> {
                if (encodedPacket instanceof String) {
                    final String encodingLength = Integer.toString(((String) encodedPacket).length(), 10);
                    final byte[] sizeBuffer = new byte[encodingLength.length() + 2];

                    sizeBuffer[0] = (byte)0; // is a string
                    for (int i = 0; i < encodingLength.length(); i ++) {
                        sizeBuffer[i + 1] = (byte)(encodingLength.charAt(i) - '0');
                    }
                    sizeBuffer[sizeBuffer.length - 1] = (byte)255;
                    results.add(MyParser.concatBuffer(sizeBuffer, ((String) encodedPacket).getBytes(StandardCharsets.UTF_8)));
                } else {
                    final String encodingLength = String.valueOf(((byte[]) encodedPacket).length);
                    final byte[] sizeBuffer = new byte[encodingLength.length() + 2];

                    sizeBuffer[0] = (byte)1; // is binary
                    for (int i = 0; i < encodingLength.length(); i ++) {
                        sizeBuffer[i + 1] = (byte)(encodingLength.charAt(i) - '0');
                    }
                    sizeBuffer[sizeBuffer.length - 1] = (byte)255;
                    results.add(MyParser.concatBuffer(sizeBuffer, (byte[]) encodedPacket));
                }
            });
        }

        callback.call(MyParser.concatBuffer(results.toArray(new byte[results.size()][])));
    }

    /**
     * Decode a packet received from transport.
     *
     * @param data Data received from transport.
     * @return Packet decoded from data.
     */
    public static Packet<?> decodePacket(Object data) {
        if(data == null) {
            return ERROR_PACKET;
        }

        if(data instanceof String) {
            final String stringData = (String) data;
            if(stringData.charAt(0) == 'b') {
                final Packet<byte[]> packet = new Packet<>(PACKETS_REVERSE.get(
                        Integer.parseInt(String.valueOf(stringData.charAt(1)))));
                packet.data = Base64.getDecoder().decode(stringData.substring(2));
                return packet;
            } else {
                final Packet<String> packet = new Packet<>(PACKETS_REVERSE.get(
                        Integer.parseInt(String.valueOf(stringData.charAt(0)))));
                packet.data = stringData.substring(1);
                return packet;
            }
        } else if(data instanceof byte[]) {
            final byte[] byteData = (byte[]) data;
            final Packet<byte[]> packet = new Packet<>(PACKETS_REVERSE.get((int) byteData[0]));
            packet.data = new byte[byteData.length - 1];
            System.arraycopy(byteData, 1, packet.data, 0, packet.data.length);
            return packet;
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
    public static void decodePayload(Object data, Parser.DecodePayloadCallback<Object> callback) {
        assert callback != null;

        final ArrayList<Packet<?>> packets = new ArrayList<>();
        if(data instanceof String) {
            final String stringData = (String) data;
            for (int payloadIdx = 0; payloadIdx < stringData.length(); ) {
                final int separatorIdx = stringData.indexOf(':', payloadIdx);
                if(separatorIdx < 0) {
                    throw new IllegalArgumentException("Invalid payload: " + stringData);
                }

                int length = Integer.parseInt(stringData.substring(payloadIdx, separatorIdx));
                String packetData = stringData.substring(
                        separatorIdx + 1,
                        length + separatorIdx + 1);
                packets.add(decodePacket(packetData));

                payloadIdx = length + separatorIdx + 1;
            }
        } else if(data instanceof byte[]) {
            final byte[] byteData = (byte[]) data;
            for (int payloadIdx = 0; payloadIdx < byteData.length; ) {
                final boolean isBinary = (byteData[payloadIdx] == 1);
                final int lengthStartIdx = payloadIdx + 1;
                int lengthEndIdx = lengthStartIdx;
                while (byteData[lengthEndIdx] != -1) {
                    lengthEndIdx++;
                }

                StringBuilder lengthString = new StringBuilder();
                for (int l = lengthStartIdx; l < lengthEndIdx; l++) {
                    char digit = (char) ('0' + byteData[l]);
                    lengthString.append(digit);
                }
                final int length = Integer.parseInt(lengthString.toString());

                byte[] bufferData = new byte[length];
                System.arraycopy(byteData, lengthEndIdx + 1, bufferData, 0, bufferData.length);

                Packet<?> packet;
                if(isBinary) {
                    packet = decodePacket(bufferData);
                } else {
                    packet = decodePacket(new String(bufferData, StandardCharsets.UTF_8));
                }
                packets.add(packet);

                payloadIdx = lengthEndIdx + length + 1;
            }
        }

        for (int i = 0; i < packets.size(); i++) {
            if(!callback.call((Packet<Object>) packets.get(i), i, packets.size())) {
                return;
            }
        }
    }

    private static void encodeByteArray(Packet<byte[]> packet, boolean supportsBinary, Parser.EncodeCallback<Object> callback) {
        if (supportsBinary) {
            byte[] data = packet.data;
            byte[] resultArray = new byte[1 + data.length];
            resultArray[0] = PACKETS.get(packet.type).byteValue();
            System.arraycopy(data, 0, resultArray, 1, data.length);
            callback.call(resultArray);
        } else {
            String resultBuilder = "b" +
                    PACKETS.get(packet.type).byteValue() +
                    Base64.getEncoder().encodeToString(packet.data);
            callback.call(resultBuilder);
        }
    }

    private static String setLengthHeader(String message) {
        return message.length() + ":" + message;
    }
}