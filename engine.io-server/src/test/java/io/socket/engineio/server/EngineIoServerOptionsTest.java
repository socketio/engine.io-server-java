package io.socket.engineio.server;

import io.socket.engineio.server.parser.Packet;
import org.junit.Assert;
import org.junit.Test;

public final class EngineIoServerOptionsTest {

    @Test(expected = IllegalStateException.class)
    public void testDefaultLocked1() {
        EngineIoServerOptions.DEFAULT.setPingTimeout(0);
    }

    @Test(expected = IllegalStateException.class)
    public void testDefaultLocked2() {
        EngineIoServerOptions.DEFAULT.setPingInterval(0);
    }

    @Test(expected = IllegalStateException.class)
    public void testDefaultLocked3() {
        EngineIoServerOptions.DEFAULT.setAllowedCorsOrigins(EngineIoServerOptions.ALLOWED_CORS_ORIGIN_ALL);
    }

    @Test(expected = IllegalStateException.class)
    public void testDefaultLocked4() {
        EngineIoServerOptions.DEFAULT.setInitialPacket(null);
    }

    @Test
    public void testSetAllowedCorsOrigins() {
        EngineIoServerOptions options = EngineIoServerOptions.newFromDefault();

        String[] origins = new String[] {
                "https://www.example.com",
                "https://www.example.org",
                "https://example.com",
                "https://example.org",
        };
        options.setAllowedCorsOrigins(origins);
        Assert.assertArrayEquals(origins, options.getAllowedCorsOrigins());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetInitialPacket_error1() {
        EngineIoServerOptions.newFromDefault().setInitialPacket(new Packet<>(Packet.PING));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetInitialPacket_error2() {
        EngineIoServerOptions.newFromDefault().setInitialPacket(new Packet<>(Packet.MESSAGE));
    }

    @Test(expected = IllegalStateException.class)
    public void testLock() {
        EngineIoServerOptions options = EngineIoServerOptions.newFromDefault();
        options.setPingInterval(500);
        options.lock();

        options.setPingTimeout(1000);
    }
}