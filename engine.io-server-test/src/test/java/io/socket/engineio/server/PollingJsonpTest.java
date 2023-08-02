package io.socket.engineio.server;

import io.socket.engineio.server.parser.Packet;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.events.AbstractWebDriverEventListener;
import org.openqa.selenium.support.events.EventFiringWebDriver;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

/*
 * TODO: Find a way to test this.
 */
@Ignore
public final class PollingJsonpTest {

    /*private static SeleniumServer sSeleniumServer;*/

    /*@Before
    public void setup() {
        final StandaloneConfiguration configuration = new StandaloneConfiguration();
        configuration.port = 4444;

        sSeleniumServer = new SeleniumServer(configuration);
        sSeleniumServer.boot();
    }

    @After
    public void teardown() {
        sSeleniumServer.stop();
    }*/

    @Test
    public void echoTest_string() throws Exception {
        final ServerWrapper serverWrapper = new ServerWrapper();

        try {
            serverWrapper.startServer();
            serverWrapper.getEngineIoServer().on("connection", args -> {
                final EngineIoSocket socket = (EngineIoSocket) args[0];
                socket.on("message", args1 -> {
                    Packet packet = new Packet(Packet.MESSAGE);
                    packet.data = args1[0];
                    socket.send(packet);
                });
            });

            assertEquals(0, executeScriptInBrowser(serverWrapper, "src/test/resources/testPollingJsonp_echo_string.js"));
        } finally {
            serverWrapper.stopServer();
        }
    }

    @Test
    public void reverseEchoTest() throws Exception {
        final ServerWrapper serverWrapper = new ServerWrapper();
        try {
            serverWrapper.startServer();
            serverWrapper.getEngineIoServer().on("connection", args -> {
                final EngineIoSocket socket = (EngineIoSocket) args[0];
                final String echoMessage = PollingTest.class.getSimpleName() + System.currentTimeMillis();
                socket.on("message", args1 -> assertEquals(echoMessage, args1[0]));

                Packet packet = new Packet(Packet.MESSAGE);
                packet.data = echoMessage;
                socket.send(packet);
            });

            assertEquals(0, executeScriptInBrowser(serverWrapper, "src/test/resources/testPollingJsonp_reverseEcho.js"));
        } finally {
            serverWrapper.stopServer();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private int executeScriptInBrowser(ServerWrapper serverWrapper, String script) throws IOException {
        class ScriptResult {
            int result = -1;
        }

        final FirefoxOptions firefoxOptions = new FirefoxOptions();
        firefoxOptions.addArguments("-headless");

        final FirefoxDriver firefoxDriver = new FirefoxDriver(firefoxOptions);
        final EventFiringWebDriver eventFiringWebDriver = new EventFiringWebDriver(firefoxDriver);
        final ScriptResult scriptResult = new ScriptResult();

        try (FileInputStream fis = new FileInputStream(script)) {
            final byte[] scriptBytes = new byte[fis.available()];
            fis.read(scriptBytes);
            final String scriptContent = new String(scriptBytes, StandardCharsets.UTF_8);

            eventFiringWebDriver.get("http://127.0.0.1:" + serverWrapper.getPort() + "/testPollingJsonp.html");
            eventFiringWebDriver.register(new AbstractWebDriverEventListener() {

                @Override
                public void afterScript(String script, WebDriver driver) {
                    final String url = eventFiringWebDriver.getCurrentUrl();
                    if (url.startsWith("http://www.example.com/?result=")) {
                        scriptResult.result = Integer.parseInt(url.substring("http://www.example.com/?result=".length()));
                    }
                }
            });
            eventFiringWebDriver.executeScript(scriptContent, serverWrapper.getPort());

            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignore) {
            }

            return scriptResult.result;
        } finally {
            firefoxDriver.close();
        }
    }
}
