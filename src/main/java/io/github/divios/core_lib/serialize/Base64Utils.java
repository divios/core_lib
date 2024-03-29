package io.github.divios.core_lib.serialize;

import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.util.Base64;

public class Base64Utils {

    public static String encode(String buf) {
        return encode(buf.getBytes());
    }

    public static String encode(byte[] buf) {
        return Base64.getEncoder().encodeToString(buf);
    }

    public static String decode(String src) {
        return new String(decodeBytes(src));
    }

    private static byte[] decodeBytes(String src) {
        try {
            return Base64.getDecoder().decode(src);
        } catch (IllegalArgumentException e) {
            // compat with the previously used base64 encoder
            try {
                return Base64Coder.decodeLines(src);
            } catch (Exception ignored) {
                throw e;
            }
        }
    }

    private Base64Utils() {}
}
