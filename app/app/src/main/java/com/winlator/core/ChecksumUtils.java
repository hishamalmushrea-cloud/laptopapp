package com.winlator.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Streaming SHA-256 helpers.
 *
 * Pure Java on purpose (no android.* imports) so it can be covered by plain JVM
 * unit tests - see app/app/src/test/java/com/winlator/core/ChecksumUtilsTest.java.
 */
public abstract class ChecksumUtils {
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /** @return lowercase hex sha256 of the file, or null when it cannot be read */
    public static String sha256(File file) {
        if (file == null || !file.isFile()) return null;

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
            return null;
        }

        byte[] buffer = new byte[64 * 1024];
        try (InputStream in = new FileInputStream(file)) {
            int read;
            while ((read = in.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        catch (IOException e) {
            return null;
        }

        byte[] bytes = digest.digest();
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            out[i * 2] = HEX[(bytes[i] >> 4) & 0x0f];
            out[i * 2 + 1] = HEX[bytes[i] & 0x0f];
        }
        return new String(out);
    }

    /** Constant-time-ish comparison; null-safe. */
    public static boolean matches(File file, String expectedSha256) {
        if (expectedSha256 == null || expectedSha256.isEmpty()) return false;
        String actual = sha256(file);
        return actual != null && actual.equalsIgnoreCase(expectedSha256.trim());
    }
}
