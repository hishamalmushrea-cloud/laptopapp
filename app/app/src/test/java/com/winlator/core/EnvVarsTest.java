package com.winlator.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * EnvVars is fed from user/imported "envVars" strings, so a malformed token must not
 * crash it (it previously threw StringIndexOutOfBoundsException on a token without '=').
 */
public class EnvVarsTest {
    @Test
    public void parsesValidPairs() {
        EnvVars e = new EnvVars("WINEDEBUG=-all LANG=en");
        assertEquals("-all", e.get("WINEDEBUG"));
        assertEquals("en", e.get("LANG"));
    }

    @Test
    public void malformedTokenWithoutEqualsDoesNotCrash() {
        EnvVars e = new EnvVars("GOOD=1 BROKEN BAD2=2");
        assertEquals("1", e.get("GOOD"));
        assertEquals("2", e.get("BAD2"));
        assertFalse(e.has("BROKEN"));
    }

    @Test
    public void emptyNameIsSkipped() {
        EnvVars e = new EnvVars("=novalue OK=1");
        assertEquals("1", e.get("OK"));
        for (String key : e) assertNotEquals("", key);
    }

    @Test
    public void valueMayContainEquals() {
        EnvVars e = new EnvVars("A=b=c");
        assertEquals("b=c", e.get("A"));
    }

    @Test
    public void missingKeyReturnsEmpty() {
        EnvVars e = new EnvVars();
        assertTrue(e.isEmpty());
        assertEquals("", e.get("NOPE"));
    }

    @Test
    public void roundTripsToString() {
        EnvVars e = new EnvVars();
        e.put("A", 1);
        e.put("B", "2");
        assertEquals("A=1 B=2", e.toString());
    }

    @Test
    public void removeAndHas() {
        EnvVars e = new EnvVars("A=1");
        assertTrue(e.has("A"));
        e.remove("A");
        assertFalse(e.has("A"));
        assertTrue(e.isEmpty());
    }
}
