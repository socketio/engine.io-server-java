package io.socket.engineio.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class TestUtils {

    @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
    public static <T> T runScriptAndGetOutput(String script, Object input, Class<T> outputClass) {
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

        final String nodeExec = (System.getenv("NODE_PATH") != null)? System.getenv("NODE_PATH") : "node";

        try {
            Process process = Runtime.getRuntime().exec(nodeExec + " " + script);

            OutputStream processOutputStream = process.getOutputStream();
            processOutputStream.write(nodeInputBytes);
            processOutputStream.close();

            process.waitFor();

            if (process.exitValue() != 0) {
                throw new RuntimeException("Script exited with code: " + process.exitValue());
            }

            InputStream processInputStream = process.getInputStream();
            byte[] result = new byte[processInputStream.available()];
            processInputStream.read(result);
            processInputStream.close();

            if(outputClass.equals(String.class)) {
                @SuppressWarnings("unchecked") T t = (T) new String(result, StandardCharsets.UTF_8);
                return t;
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
