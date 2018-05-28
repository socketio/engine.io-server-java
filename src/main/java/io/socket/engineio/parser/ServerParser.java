package io.socket.engineio.parser;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public final class ServerParser {

    public static final int PROTOCOL = Parser.PROTOCOL;

    private static final Map<String, Integer> packets = new HashMap<String, Integer>() {{
        put(Packet.OPEN, 0);
        put(Packet.CLOSE, 1);
        put(Packet.PING, 2);
        put(Packet.PONG, 3);
        put(Packet.MESSAGE, 4);
        put(Packet.UPGRADE, 5);
        put(Packet.NOOP, 6);
    }};

    /*private static final Map<Integer, String> packetsList = new HashMap<>();
    static {
        for (Map.Entry<String, Integer> entry : packets.entrySet()) {
            packetsList.put(entry.getValue(), entry.getKey());
        }
    }*/

    /*private static Packet<String> err = new Packet<>(Packet.ERROR, "parser error");*/

    private ServerParser() {}

    public static void encodePacket(Packet packet, boolean supportsBinary, Parser.EncodeCallback callback) {
        if (packet.data instanceof byte[]) {
            @SuppressWarnings("unchecked")
            Packet<byte[]> packetToEncode = packet;
            encodeByteArray(packetToEncode, supportsBinary, callback);
        } else {
            String encoded = String.valueOf(packets.get(packet.type));

            if (null != packet.data) {
                encoded += String.valueOf(packet.data);
            }

            @SuppressWarnings("unchecked")
            Parser.EncodeCallback<String> tempCallback = callback;
            tempCallback.call(encoded);
        }
    }

    @SuppressWarnings("unchecked")
    private static void encodeByteArray(Packet<byte[]> packet, boolean supportsBinary, Parser.EncodeCallback callback) {
        if (supportsBinary) {
            byte[] data = packet.data;
            byte[] resultArray = new byte[1 + data.length];
            resultArray[0] = packets.get(packet.type).byteValue();
            System.arraycopy(data, 0, resultArray, 1, data.length);
            callback.call(resultArray);
        } else {
            String resultBuilder = "b" +
                    packets.get(packet.type).byteValue() +
                    DatatypeConverter.printBase64Binary(packet.data);
            callback.call(resultBuilder);
        }
    }

    @SuppressWarnings("unchecked")
    public static void encodePayload(Packet[] packets, boolean supportsBinary, Parser.EncodeCallback callback) {
        boolean isBinary = false;
        for (Packet packet : packets) {
            if (packet.data instanceof byte[]) {
                isBinary = true;
                break;
            }
        }

        if (isBinary && supportsBinary) {
            Parser.EncodeCallback<byte[]> _callback = (Parser.EncodeCallback<byte[]>) callback;
            encodePayloadAsBinary(packets, _callback);
            return;
        }

        if (packets.length == 0) {
            callback.call("0:");
            return;
        }

        final StringBuilder result = new StringBuilder();

        for (Packet packet : packets) {
            encodePacket(packet, false, new Parser.EncodeCallback() {
                @Override
                public void call(Object data) {
                    result.append(setLengthHeader((String) data));
                }
            });
        }

        callback.call(result.toString());
    }

    private static void encodePayloadAsBinary(Packet[] packets, Parser.EncodeCallback<byte[]> callback) {
        if (packets.length == 0) {
            callback.call(new byte[0]);
            return;
        }

        final ArrayList<byte[]> results = new ArrayList<>(packets.length);

        for (Packet packet : packets) {
            encodePacket(packet, true, new Parser.EncodeCallback() {
                @Override
                public void call(Object packet) {
                    if (packet instanceof String) {
                        String encodingLength = String.valueOf(((String) packet).length());
                        byte[] sizeBuffer = new byte[encodingLength.length() + 2];

                        sizeBuffer[0] = (byte)0; // is a string
                        for (int i = 0; i < encodingLength.length(); i ++) {
                            sizeBuffer[i + 1] = (byte)Character.getNumericValue(encodingLength.charAt(i));
                        }
                        sizeBuffer[sizeBuffer.length - 1] = (byte)255;
                        results.add(Buffer.concat(new byte[][] {
                                sizeBuffer,
                                ((String)packet).getBytes(StandardCharsets.UTF_8)
                        }));
                    } else {
                        String encodingLength = String.valueOf(((byte[])packet).length);
                        byte[] sizeBuffer = new byte[encodingLength.length() + 2];
                        sizeBuffer[0] = (byte)1; // is binary
                        for (int i = 0; i < encodingLength.length(); i ++) {
                            sizeBuffer[i + 1] = (byte)Character.getNumericValue(encodingLength.charAt(i));
                        }
                        sizeBuffer[sizeBuffer.length - 1] = (byte)255;
                        results.add(Buffer.concat(new byte[][] {
                                sizeBuffer,
                                (byte[])packet
                        }));
                    }
                }
            });
        }

        callback.call(Buffer.concat(results.toArray(new byte[results.size()][])));
    }

    private static String setLengthHeader(String message) {
        return message.length() + ":" + message;
    }
}