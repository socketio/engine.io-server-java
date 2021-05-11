package io.socket.yeast;

import java.security.SecureRandom;

public final class ServerYeast {

    /**
     * A ThreadLocal is used to improve thread performance since {@link SecureRandom} do not perform well with
     * thread contention.
     */
    private static final ThreadLocal<SecureRandom> THREAD_RANDOM = new ThreadLocal<>();
    private static final char[] alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_".toCharArray();

    public static String yeast() {
        SecureRandom secureRandom = THREAD_RANDOM.get();
        if (secureRandom == null) {
            secureRandom = new SecureRandom();
            THREAD_RANDOM.set(secureRandom);
        }

        return encode(secureRandom.nextLong() & 0x7fffffffffffffffL);
    }

    public static String encode(long num) {
        final StringBuilder encoded = new StringBuilder();
        long dividedNum = num;
        do {
            encoded.insert(0, alphabet[(int)(dividedNum % alphabet.length)]);
            dividedNum = dividedNum / alphabet.length;
        } while (dividedNum > 0);

        return encoded.toString();
    }
}
