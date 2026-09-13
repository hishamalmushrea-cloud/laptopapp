package com.winlator.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * First unit tests in this project. ChecksumUtils is deliberately free of android.*
 * imports so it runs on the plain JVM (testDebugUnitTest) with no emulator.
 *
 * Expected digests were computed independently with Python's hashlib.
 */
public class ChecksumUtilsTest {
    private static final String SHA_ABC = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
    private static final String SHA_EMPTY = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final String SHA_200K_A = "2287d207f24a941ff3b56c04c8a25ad56b63e3023207b3bb5b4ac0c9869d74be";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File fileWith(byte[] data) throws IOException {
        File file = tmp.newFile();
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(data);
        }
        return file;
    }

    @Test
    public void sha256_matchesKnownVector() throws IOException {
        assertEquals(SHA_ABC, ChecksumUtils.sha256(fileWith("abc".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    public void sha256_ofEmptyFile() throws IOException {
        assertEquals(SHA_EMPTY, ChecksumUtils.sha256(fileWith(new byte[0])));
    }

    @Test
    public void sha256_streamsFilesLargerThanTheReadBuffer() throws IOException {
        // Buffer is 64 KiB, so this exercises the read loop across several iterations.
        byte[] data = new byte[200_000];
        Arrays.fill(data, (byte) 'a');
        assertEquals(SHA_200K_A, ChecksumUtils.sha256(fileWith(data)));
    }

    @Test
    public void sha256_returnsNullForMissingOrNullFile() throws IOException {
        assertNull(ChecksumUtils.sha256(null));
        assertNull(ChecksumUtils.sha256(new File(tmp.getRoot(), "does-not-exist")));
        File dir = tmp.newFolder();
        assertNull("a directory is not a file", ChecksumUtils.sha256(dir));
    }

    @Test
    public void matches_acceptsTheRightDigestIgnoringCase() throws IOException {
        File file = fileWith("abc".getBytes(StandardCharsets.UTF_8));
        assertTrue(ChecksumUtils.matches(file, SHA_ABC));
        assertTrue(ChecksumUtils.matches(file, SHA_ABC.toUpperCase()));
        assertTrue("surrounding whitespace is tolerated", ChecksumUtils.matches(file, "  " + SHA_ABC + "\n"));
    }

    @Test
    public void matches_rejectsAWrongOrEmptyDigest() throws IOException {
        File file = fileWith("abc".getBytes(StandardCharsets.UTF_8));
        assertFalse(ChecksumUtils.matches(file, SHA_EMPTY));
        assertFalse(ChecksumUtils.matches(file, null));
        assertFalse(ChecksumUtils.matches(file, ""));
        assertFalse(ChecksumUtils.matches(file, "not-a-hex-digest"));
    }

    @Test
    public void matches_rejectsWhenTheFileChangedByOneByte() throws IOException {
        // This is the exact scenario the component downloader must catch: a valid
        // download that was tampered with in transit.
        byte[] data = "winlator-component".getBytes(StandardCharsets.UTF_8);
        File good = fileWith(data);
        String expected = ChecksumUtils.sha256(good);
        assertTrue(ChecksumUtils.matches(good, expected));

        data[data.length - 1] ^= 0x01;
        File tampered = fileWith(data);
        assertFalse(ChecksumUtils.matches(tampered, expected));
    }
}
