package io.socket.engineio.parser;

import org.json.JSONArray;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Test {@link ServerParser} against reference JS implementation.
 */
public final class ServerParserTest {

    /**
     * This is just to remove the warning on the field.
     */
    @Test
    public void testProtocolVersion() {
        assertEquals(3, ServerParser.PROTOCOL);
    }

    @Test
    public void testEncodePacket_string() {
        final Packet<String> packet = new Packet<>(Packet.MESSAGE);

        packet.data = "Hello World";
        ServerParser.encodePacket(packet, false, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                String result = runScriptAndGetOutput("src/test/resources/testEncodePacket_string.js", packet.data, String.class);
                assertEquals(result, data);
            }
        });

        packet.data = "Engine.IO";
        ServerParser.encodePacket(packet, false, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                String result = runScriptAndGetOutput("src/test/resources/testEncodePacket_string.js", packet.data, String.class);
                assertEquals(result, data);
            }
        });
    }

    @Test
    public void testEncodePacket_binary() {
        final Packet<byte[]> packet = new Packet<>(Packet.MESSAGE);

        packet.data = new byte[] { 1, 2, 3, 4, 5 };
        ServerParser.encodePacket(packet, true, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                byte[] result = runScriptAndGetOutput("src/test/resources/testEncodePacket_binary.js", packet.data, byte[].class);
                assertArrayEquals(result, (byte[]) data);
            }
        });
    }

    @Test
    public void testEncodePacket_base64() {
        final Packet<byte[]> packet = new Packet<>(Packet.MESSAGE);

        packet.data = new byte[] { 1, 2, 3, 4, 5 };
        ServerParser.encodePacket(packet, false, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                String result = runScriptAndGetOutput("src/test/resources/testEncodePacket_base64.js", packet.data, String.class);
                assertEquals(result, data);
            }
        });
    }

    @Test
    public void testEncodePayload_string_empty() {
        final Packet[] packets = new Packet[0];
        final JSONArray jsonArray = new JSONArray();

        ServerParser.encodePayload(packets, false, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                String result = runScriptAndGetOutput("src/test/resources/testEncodePayload_string.js", jsonArray.toString(), String.class);
                assertEquals(result, data);
            }
        });
    }

    @Test
    public void testEncodePayload_string() {
        final String[] messages = new String[] {
                "Hello World",
                "Engine.IO"
        };
        final Packet[] packets = new Packet[messages.length];
        final JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < messages.length; i++) {
            jsonArray.put(i, messages[i]);

            Packet<String> packet = new Packet<>(Packet.MESSAGE);
            packet.data = messages[i];
            packets[i] = packet;
        }

        ServerParser.encodePayload(packets, false, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                String result = runScriptAndGetOutput("src/test/resources/testEncodePayload_string.js", jsonArray.toString(), String.class);
                assertEquals(result, data);
            }
        });
    }

    @Test
    public void testEncodePayload_binary_empty() {
        final byte[][] messages = new byte[][] {
        };
        final Packet[] packets = new Packet[messages.length];
        final JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < messages.length; i++) {
            jsonArray.put(i, messages[i]);

            Packet<byte[]> packet = new Packet<>(Packet.MESSAGE);
            packet.data = messages[i];
            packets[i] = packet;
        }

        ServerParser.encodePayload(packets, true, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                byte[] result = runScriptAndGetOutput("src/test/resources/testEncodePayload_binary.js", jsonArray.toString(), byte[].class);
                assertArrayEquals(result, ((String) data).getBytes());
            }
        });
    }

    @Test
    public void testEncodePayload_binary() {
        final byte[][] messages = new byte[][] {
                {1, 2, 3, 4, 5}, {11, 12, 13, 14, 15}
        };
        final Packet[] packets = new Packet[messages.length];
        final JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < messages.length; i++) {
            jsonArray.put(i, messages[i]);

            Packet<byte[]> packet = new Packet<>(Packet.MESSAGE);
            packet.data = messages[i];
            packets[i] = packet;
        }

        ServerParser.encodePayload(packets, true, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                byte[] result = runScriptAndGetOutput("src/test/resources/testEncodePayload_binary.js", jsonArray.toString(), byte[].class);
                assertArrayEquals(result, (byte[]) data);
            }
        });
    }

    @Test
    public void testEncodePayload_base64() {
        final byte[][] messages = new byte[][] {
                {1, 2, 3, 4, 5}, {11, 12, 13, 14, 15}
        };
        final Packet[] packets = new Packet[messages.length];
        final JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < messages.length; i++) {
            jsonArray.put(i, messages[i]);

            Packet<byte[]> packet = new Packet<>(Packet.MESSAGE);
            packet.data = messages[i];
            packets[i] = packet;
        }

        ServerParser.encodePayload(packets, false, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                String result = runScriptAndGetOutput("src/test/resources/testEncodePayload_base64.js", jsonArray.toString(), String.class);
                assertEquals(result, data);
            }
        });
    }

    @Test
    public void testDecodePacket_string() {
        final Packet<String> packetOriginal = new Packet<>(Packet.MESSAGE, "Engine.IO");
        ServerParser.encodePacket(packetOriginal, false, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                Packet packetDecoded = ServerParser.decodePacket(data);
                assertEquals(Packet.MESSAGE, packetDecoded.type);
                assertEquals(String.class, packetDecoded.data.getClass());
                assertEquals(packetOriginal.data, packetDecoded.data);
            }
        });
    }

    @Test
    public void testDecodePacket_binary() {
        final Packet<byte[]> packetOriginal = new Packet<>(Packet.MESSAGE, "Engine.IO".getBytes(StandardCharsets.UTF_8));
        ServerParser.encodePacket(packetOriginal, true, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                Packet packetDecoded = ServerParser.decodePacket(data);
                assertEquals(Packet.MESSAGE, packetDecoded.type);
                assertEquals(byte[].class, packetDecoded.data.getClass());
                assertArrayEquals(packetOriginal.data, (byte[]) packetDecoded.data);
            }
        });
    }

    @Test
    public void testDecodePacket_base64() {
        final Packet<byte[]> packetOriginal = new Packet<>(Packet.MESSAGE, "Engine.IO".getBytes(StandardCharsets.UTF_8));
        ServerParser.encodePacket(packetOriginal, false, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                Packet packetDecoded = ServerParser.decodePacket(data);
                assertEquals(Packet.MESSAGE, packetDecoded.type);
                assertEquals(byte[].class, packetDecoded.data.getClass());
                assertArrayEquals(packetOriginal.data, (byte[]) packetDecoded.data);
            }
        });
    }

    @Test
    public void testDecodePayload_string() {
        final Packet[] packets = new Packet[] {
                new Packet<String>(Packet.MESSAGE),
                new Packet<String>(Packet.MESSAGE)
        };
        packets[0].data = "Engine.IO";
        packets[1].data = "Test.Data";
        ServerParser.encodePayload(packets, false, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                assertEquals(String.class, data.getClass());

                ServerParser.decodePayload(data, new Parser.DecodePayloadCallback() {
                    @Override
                    public boolean call(Packet packet, int index, int total) {
                        Packet originalPacket = packets[index];
                        assertEquals(originalPacket.data.getClass(), packet.data.getClass());
                        assertEquals(originalPacket.type, packet.type);
                        assertEquals(originalPacket.data, packet.data);

                        return true;
                    }
                });
            }
        });
    }

    @Test
    public void testDecodePayload_binary() {
        final Packet[] packets = new Packet[] {
                new Packet<byte[]>(Packet.MESSAGE),
                new Packet<byte[]>(Packet.MESSAGE)
        };
        packets[0].data = "Engine.IO".getBytes(StandardCharsets.UTF_8);
        packets[1].data = "Test.Data".getBytes(StandardCharsets.UTF_8);
        ServerParser.encodePayload(packets, true, new Parser.EncodeCallback() {
            @SuppressWarnings("Duplicates")
            @Override
            public void call(Object data) {
                assertEquals(byte[].class, data.getClass());

                ServerParser.decodePayload(data, new Parser.DecodePayloadCallback() {
                    @Override
                    public boolean call(Packet packet, int index, int total) {
                        Packet originalPacket = packets[index];
                        assertEquals(originalPacket.data.getClass(), packet.data.getClass());
                        assertEquals(originalPacket.type, packet.type);
                        assertArrayEquals((byte[]) originalPacket.data, (byte[]) packet.data);

                        return true;
                    }
                });
            }
        });
    }

    @Test
    public void testDecodePayload_base64() {
        final Packet[] packets = new Packet[] {
                new Packet<byte[]>(Packet.MESSAGE),
                new Packet<byte[]>(Packet.MESSAGE)
        };
        packets[0].data = "Engine.IO".getBytes(StandardCharsets.UTF_8);
        packets[1].data = "Test.Data".getBytes(StandardCharsets.UTF_8);
        ServerParser.encodePayload(packets, false, new Parser.EncodeCallback() {
            @SuppressWarnings("Duplicates")
            @Override
            public void call(Object data) {
                assertEquals(String.class, data.getClass());

                ServerParser.decodePayload(data, new Parser.DecodePayloadCallback() {
                    @Override
                    public boolean call(Packet packet, int index, int total) {
                        Packet originalPacket = packets[index];
                        assertEquals(originalPacket.data.getClass(), packet.data.getClass());
                        assertEquals(originalPacket.type, packet.type);
                        assertArrayEquals((byte[]) originalPacket.data, (byte[]) packet.data);

                        return true;
                    }
                });
            }
        });
    }

    @Test
    public void testDecodePayload_mixed_binary() {
        final Packet[] packets = new Packet[] {
                new Packet<String>(Packet.MESSAGE),
                new Packet<byte[]>(Packet.MESSAGE)
        };
        packets[0].data = "Engine.IO";
        packets[1].data = "Test.Data".getBytes(StandardCharsets.UTF_8);
        ServerParser.encodePayload(packets, true, new Parser.EncodeCallback() {
            @SuppressWarnings("Duplicates")
            @Override
            public void call(Object data) {
                assertEquals(byte[].class, data.getClass());

                ServerParser.decodePayload(data, new Parser.DecodePayloadCallback() {
                    @Override
                    public boolean call(Packet packet, int index, int total) {
                        Packet originalPacket = packets[index];
                        assertEquals(originalPacket.data.getClass(), packet.data.getClass());
                        assertEquals(originalPacket.type, packet.type);

                        if (originalPacket.data instanceof byte[]) {
                            assertArrayEquals((byte[]) originalPacket.data, (byte[]) packet.data);
                        } else {
                            assertEquals(originalPacket.data, packet.data);
                        }

                        return true;
                    }
                });
            }
        });
    }

    @Test
    public void testDecodePayload_mixed_base64() {
        final Packet[] packets = new Packet[] {
                new Packet<String>(Packet.MESSAGE),
                new Packet<byte[]>(Packet.MESSAGE)
        };
        packets[0].data = "Engine.IO";
        packets[1].data = "Test.Data".getBytes(StandardCharsets.UTF_8);
        ServerParser.encodePayload(packets, false, new Parser.EncodeCallback() {
            @SuppressWarnings("Duplicates")
            @Override
            public void call(Object data) {
                assertEquals(String.class, data.getClass());

                ServerParser.decodePayload(data, new Parser.DecodePayloadCallback() {
                    @Override
                    public boolean call(Packet packet, int index, int total) {
                        Packet originalPacket = packets[index];
                        assertEquals(originalPacket.data.getClass(), packet.data.getClass());
                        assertEquals(originalPacket.type, packet.type);

                        if (originalPacket.data instanceof byte[]) {
                            assertArrayEquals((byte[]) originalPacket.data, (byte[]) packet.data);
                        } else {
                            assertEquals(originalPacket.data, packet.data);
                        }

                        return true;
                    }
                });
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> T runScriptAndGetOutput(String script, Object input, Class<T> outputClass) {
        byte[] nodeInputBytes;
        if (input instanceof String) {
            nodeInputBytes = ((String) input).getBytes(StandardCharsets.UTF_8);
        } else if (input instanceof byte[]) {
            nodeInputBytes = (byte[]) input;
        } else {
            throw new RuntimeException("Invalid input type: " + input.getClass().getSimpleName());
        }

        if(!outputClass.equals(String.class) && !outputClass.equals(byte[].class)) {
            throw new RuntimeException("Invalid output type: " + outputClass.getSimpleName());
        }

        try {
            Process process = Runtime.getRuntime().exec("node " + script);

            OutputStream processOutputStream = process.getOutputStream();
            processOutputStream.write(nodeInputBytes);
            processOutputStream.close();

            process.waitFor();

            InputStream processInputStream = process.getInputStream();
            byte[] result = new byte[processInputStream.available()];
            //noinspection ResultOfMethodCallIgnored
            processInputStream.read(result);
            processInputStream.close();

            if(outputClass.equals(String.class)) {
                return (T) new String(result, StandardCharsets.UTF_8);
            } else if (outputClass.equals(byte[].class)) {
                return (T) result;
            } else {
                throw new RuntimeException("Should never be here.");
            }
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}