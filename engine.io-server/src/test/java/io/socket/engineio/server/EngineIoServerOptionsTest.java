package io.socket.engineio.server;

import io.socket.engineio.parser.Packet;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Comparator;

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
                "http://www.example.com",
                "http://www.example.org",
                "http://example.com",
                "http://example.org",
        };
        options.setAllowedCorsOrigins(origins);

        Arrays.sort(origins, String::compareTo);

        Assert.assertArrayEquals(origins, options.getAllowedCorsOrigins());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetInitialPacket_error1() {
        EngineIoServerOptions.newFromDefault().setInitialPacket(new Packet(Packet.PING));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetInitialPacket_error2() {
        EngineIoServerOptions.newFromDefault().setInitialPacket(new Packet(Packet.MESSAGE));
    }

    @Test(expected = IllegalStateException.class)
    public void testLock() {
        EngineIoServerOptions options = EngineIoServerOptions.newFromDefault();
        options.setPingInterval(500);
        options.lock();

        options.setPingTimeout(1000);
    }
}