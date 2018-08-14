package io.socket.engineio.server;

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

        Arrays.sort(origins, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });

        Assert.assertArrayEquals(origins, options.getAllowedCorsOrigins());
    }

    @Test(expected = IllegalStateException.class)
    public void testLock() {
        EngineIoServerOptions options = EngineIoServerOptions.newFromDefault();
        options.setPingInterval(500);
        options.lock();

        options.setPingTimeout(1000);
    }
}