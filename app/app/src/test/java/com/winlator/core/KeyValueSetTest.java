package com.winlator.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * KeyValueSet parses the "a=1,b=2" strings used for env vars, wincomponents and
 * driver configs - including strings that arrive from imported or downloaded data,
 * which is exactly why the malformed-input cases below matter.
 */
public class KeyValueSetTest {

    @Test
    public void emptySetAlwaysReturnsTheFallback() {
        KeyValueSet set = new KeyValueSet("");
        assertTrue(set.isEmpty());
        assertEquals("", set.get("missing"));
        assertEquals("fallback", set.get("missing", "fallback"));
        assertEquals(7, set.getInt("missing", 7));
        assertEquals(1.5f, set.getFloat("missing", 1.5f), 0.0f);
        assertTrue(set.getBoolean("missing", true));
    }

    @Test
    public void nullObjectConstructorBehavesLikeEmpty() {
        assertTrue(new KeyValueSet((Object) null).isEmpty());
        assertEquals("fb", new KeyValueSet((Object) null).get("k", "fb"));
    }

    @Test
    public void getReadsEveryPair() {
        KeyValueSet set = new KeyValueSet("direct3d=builtin,directsound=native,xaudio=1");
        assertEquals("builtin", set.get("direct3d"));
        assertEquals("native", set.get("directsound"));
        assertEquals("1", set.get("xaudio"));
        assertEquals("", set.get("vcrun2010"));
    }

    @Test
    public void typedGettersFallBackOnUnparseableValues() {
        KeyValueSet set = new KeyValueSet("good=42,ratio=0.25,text=hello");
        assertEquals(42, set.getInt("good"));
        assertEquals(0.25f, set.getFloat("ratio"), 0.0f);
        assertEquals(-1, set.getInt("text", -1));
        assertEquals(-1.0f, set.getFloat("text", -1.0f), 0.0f);
    }

    @Test
    public void getBooleanAcceptsOneTAndTrue() {
        assertEquals(true, new KeyValueSet("a=1").getBoolean("a"));
        assertEquals(true, new KeyValueSet("a=t").getBoolean("a"));
        assertEquals(true, new KeyValueSet("a=true").getBoolean("a"));
        assertEquals(false, new KeyValueSet("a=0").getBoolean("a"));
        assertEquals(false, new KeyValueSet("a=false").getBoolean("a"));
    }

    @Test
    public void getHexStringFormatsToEightDigits() {
        assertEquals("0x000000ff", new KeyValueSet("").getHexString("k", 255));
        assertEquals("0x00000010", new KeyValueSet("k=16").getHexString("k", 0));
        assertEquals("0x00000000", new KeyValueSet("k=not-a-number").getHexString("k", 0));
    }

    @Test
    public void putAppendsNewKeysAndReplacesExistingOnes() {
        KeyValueSet set = new KeyValueSet();
        set.put("a", 1).put("b", "x");
        assertEquals("a=1,b=x", set.toString());

        set.put("a", 9);
        assertEquals("a replacement must not disturb the other pairs", "a=9,b=x", set.toString());

        set.put("c", true);
        assertEquals("a=9,b=x,c=true", set.toString());
    }

    @Test
    public void iteratorYieldsIndependentArrays() {
        // Regression: next() used to return one shared String[2], so a caller that
        // collected the elements saw the last pair repeated.
        List<String[]> collected = new ArrayList<>();
        for (String[] pair : new KeyValueSet("a=1,b=2,c=3")) collected.add(pair);

        assertEquals(3, collected.size());
        assertEquals("a", collected.get(0)[0]);
        assertEquals("1", collected.get(0)[1]);
        assertEquals("b", collected.get(1)[0]);
        assertEquals("2", collected.get(1)[1]);
        assertEquals("c", collected.get(2)[0]);
        assertEquals("3", collected.get(2)[1]);
    }

    @Test
    public void segmentWithoutEqualsDoesNotThrowOnRead() {
        // Regression: substring(index + 1, end) with end < index + 1 threw
        // StringIndexOutOfBoundsException, so one malformed pair made the whole
        // string unreadable.
        KeyValueSet set = new KeyValueSet("a=1,broken,b=2");
        assertEquals("1", set.get("a"));
        assertEquals("2", set.get("b"));

        int seen = 0;
        for (String[] ignored : set) seen++;
        assertEquals(3, seen);
    }

    @Test
    public void segmentWithoutEqualsDoesNotThrowOnWrite() {
        // Regression: indexOfKey hit substring(start, -1) on the malformed pair.
        KeyValueSet set = new KeyValueSet("a=1,broken");
        set.put("c", 3);
        assertEquals("a=1,broken,c=3", set.toString());
        assertFalse(set.isEmpty());
    }
}
