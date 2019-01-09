package io.socket.yeast;

import java.util.concurrent.atomic.AtomicLong;

public final class ServerYeast {

    private static final AtomicLong VALUE = new AtomicLong(System.currentTimeMillis());

    public static String yeast() {
        return Yeast.encode(VALUE.incrementAndGet());
    }
}
