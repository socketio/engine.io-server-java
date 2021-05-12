package io.socket.engineio.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class TestUtils {

    private TestUtils() {
    }

    static int executeScriptForResult(String script, int port) throws IOException {
        final String nodeExec = (System.getenv("NODE_PATH") != null)? System.getenv("NODE_PATH") : "node";

        Process process = Runtime.getRuntime().exec(nodeExec + " " + script, new String[] {
                "PORT=" + port
        });
        try {
            int result = process.waitFor();

            InputStream inputStream = process.getInputStream();
            byte[] buffer = new byte[inputStream.available()];
            //noinspection ResultOfMethodCallIgnored
            inputStream.read(buffer);
            System.out.println(new String(buffer, StandardCharsets.UTF_8));

            return result;
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}