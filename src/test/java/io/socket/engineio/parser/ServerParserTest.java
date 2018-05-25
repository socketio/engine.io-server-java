package io.socket.engineio.parser;

import io.socket.utf8.UTF8Exception;
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
    public void testEncodePacket_string() throws UTF8Exception {
        final Packet<String> packet = new Packet<>(Packet.MESSAGE);

        packet.data = "Hello World";
        ServerParser.encodePacket(packet, false, false, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                String result = runScriptAndGetOutput("src/test/resources/testEncodePacket_string.js", packet.data, String.class);
                assertEquals(result, data);
            }
        });

        packet.data = "Engine.IO";
        ServerParser.encodePacket(packet, false, false, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                String result = runScriptAndGetOutput("src/test/resources/testEncodePacket_string.js", packet.data, String.class);
                assertEquals(result, data);
            }
        });
    }

    @Test
    public void testEncodePacket_binary() throws UTF8Exception {
        final Packet<byte[]> packet = new Packet<>(Packet.MESSAGE);

        packet.data = new byte[] { 1, 2, 3, 4, 5 };
        ServerParser.encodePacket(packet, true, false, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                byte[] result = runScriptAndGetOutput("src/test/resources/testEncodePacket_binary.js", packet.data, byte[].class);
                assertArrayEquals(result, (byte[]) data);
            }
        });
    }

    @Test
    public void testEncodePacket_base64() throws UTF8Exception {
        final Packet<byte[]> packet = new Packet<>(Packet.MESSAGE);

        packet.data = new byte[] { 1, 2, 3, 4, 5 };
        ServerParser.encodePacket(packet, false, false, new Parser.EncodeCallback() {
            @Override
            public void call(Object data) {
                String result = runScriptAndGetOutput("src/test/resources/testEncodePacket_base64.js", packet.data, String.class);
                assertEquals(result, data);
            }
        });
    }

    @Test
    public void testEncodePayload_string_empty() throws UTF8Exception {
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
    public void testEncodePayload_string() throws UTF8Exception {
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
    public void testEncodePayload_binary_empty() throws UTF8Exception {
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
    public void testEncodePayload_binary() throws UTF8Exception {
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
    public void testEncodePayload_base64() throws UTF8Exception {
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