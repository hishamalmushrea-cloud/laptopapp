package com.winlator.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Unit tests for the pure string helpers in StringUtils. */
public class StringUtilsTest {
    @Test public void formatBytesCoversEveryUnit() {
        assertEquals("0 bytes", StringUtils.formatBytes(0));
        assertEquals("0 bytes", StringUtils.formatBytes(-5));
        assertEquals("512.00 bytes", StringUtils.formatBytes(512));
        assertEquals("1.00 KB", StringUtils.formatBytes(1024));
        assertEquals("1.00 MB", StringUtils.formatBytes(1048576));
        assertEquals("1.00 GB", StringUtils.formatBytes(1073741824L));
        assertEquals("1.00 TB", StringUtils.formatBytes(1099511627776L));
    }

    /** Regression: 1 PB (1024^5) used to throw ArrayIndexOutOfBoundsException (units[] has no PB). */
    @Test public void formatBytesDoesNotCrashBeyondTerabytes() {
        assertEquals("1024.00 TB", StringUtils.formatBytes(1125899906842624L));
        // Far beyond, still must not throw.
        assertEquals("9223372036854775807.00 TB", StringUtils.formatBytes(Long.MAX_VALUE));
    }

    @Test public void slashHelpersTrimAndAppend() {
        assertEquals("a", StringUtils.removeStartSlash("//a"));
        assertEquals("a", StringUtils.removeEndSlash("a//"));
        assertEquals("a/", StringUtils.addEndSlash("a"));
        assertEquals("a/", StringUtils.addEndSlash("a/"));
    }

    @Test public void parseNumberKeepsDigitsAndFallsBack() {
        assertEquals("123", StringUtils.parseNumber("abc123def"));
        assertEquals("1234", StringUtils.parseNumber("12a34"));
        assertEquals("", StringUtils.parseNumber(null));
        assertEquals("X", StringUtils.parseNumber("", "X"));
    }

    @Test public void parseMemorySizeSameUnitAndNonNumeric() {
        assertEquals("512", StringUtils.parseMemorySize("512 MB", "MB"));
        assertEquals("0", StringUtils.parseMemorySize("abc"));
    }

    @Test public void clearReservedCharsStripsAndHandlesNull() {
        assertEquals("abc", StringUtils.clearReservedChars("a/b:c"));
        assertEquals("", StringUtils.clearReservedChars(null));
        assertEquals("", StringUtils.clearReservedChars(""));
    }

    @Test public void repeatBuildsTheRequestedLength() {
        assertEquals("xxx", StringUtils.repeat('x', 3));
        assertEquals("", StringUtils.repeat('x', 0));
    }
}
