package io.socket.engineio.server.utils;

import java.util.Arrays;
import java.util.Locale;

public interface JsonUtils {

    /**
     * Replacements for the first 255 characters
     */
    String[] REPLACEMENTS = new String[] {
            "\\u0000", "\\u0001", "\\u0002", "\\u0003", "\\u0004", "\\u0005", "\\u0006", "\\u0007", "\\u0008", "\\u0009", "\\u000A", "\\u000B", "\\u000C", "\\u000D", "\\u000E", "\\u000F",
            "\\u0010", "\\u0011", "\\u0012", "\\u0013", "\\u0014", "\\u0015", "\\u0016", "\\u0017", "\\u0018", "\\u0019", "\\u001A", "\\u001B", "\\u001C", "\\u001D", "\\u001E", "\\u001F",
            null, null, "\\\"", null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, "\\\\", null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
    };

    String EMPTY_ARRAY = "[]";

    static String toJson(String[] array) {
        if (array.length <= 0) {
            return EMPTY_ARRAY;
        }

        return '[' +
                String.join(",", Arrays.stream(array).map(s -> "\"" + escape(s) + "\"").toArray(String[]::new)) +
                ']';
    }

    static String escape(String input) {
        if (input == null) {
            return null;
        }

        final StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            final char ch = input.charAt(i);
            if (ch < 0x80) {
                final String replacement = REPLACEMENTS[ch];
                if (replacement != null) {
                    sb.append(replacement);
                    continue;
                }
            } else if (ch < 0x100) {
                sb.append("\\u00").append(Integer.toHexString(ch).toLowerCase(Locale.ROOT));
                continue;
            }

            sb.append(ch);
        }
        return sb.toString();
    }
}
