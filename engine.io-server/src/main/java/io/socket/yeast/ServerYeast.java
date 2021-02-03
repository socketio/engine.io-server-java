package io.socket.yeast;

import java.security.SecureRandom;

public final class ServerYeast {

    /**
     * A ThreadLocal is used to improve thread performance since {@link SecureRandom} do not perform well with
     * thread contention.
     */
    private static final ThreadLocal<SecureRandom> THREAD_RANDOM = new ThreadLocal<>();

    public static String yeast() {
        SecureRandom secureRandom = THREAD_RANDOM.get();
        if (secureRandom == null) {
            secureRandom = new SecureRandom();
            THREAD_RANDOM.set(secureRandom);
        }

        return Yeast.encode(secureRandom.nextLong() & 0x7fffffffffffffffL);
    }
}
