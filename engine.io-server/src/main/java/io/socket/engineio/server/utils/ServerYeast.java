package io.socket.engineio.server.utils;

import java.security.SecureRandom;

public interface ServerYeast {

    /**
     * A ThreadLocal is used to improve thread performance since {@link SecureRandom} do not perform well with
     * thread contention.
     */
    ThreadLocal<SecureRandom> THREAD_RANDOM = new ThreadLocal<>();
    char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_".toCharArray();

    static String yeast() {
        SecureRandom secureRandom = THREAD_RANDOM.get();
        if (secureRandom == null) {
            secureRandom = new SecureRandom();
            THREAD_RANDOM.set(secureRandom);
        }

        return encode(secureRandom.nextLong() & 0x7fffffffffffffffL);
    }

    static String encode(long num) {
        final StringBuilder encoded = new StringBuilder();
        long dividedNum = num;
        do {
            encoded.insert(0, ALPHABET[(int)(dividedNum % ALPHABET.length)]);
            dividedNum = dividedNum / ALPHABET.length;
        } while (dividedNum > 0);

        return encoded.toString();
    }
}
