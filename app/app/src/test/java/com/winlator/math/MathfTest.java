package com.winlator.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Pure-math coverage; farey() is excluded because it needs android.util.Rational. */
public class MathfTest {
    @Test public void clampBoundsValues() {
        assertEquals(3f, Mathf.clamp(5f, 0f, 3f), 0f);
        assertEquals(0f, Mathf.clamp(-1f, 0f, 3f), 0f);
        assertEquals(2f, Mathf.clamp(2f, 0f, 3f), 0f);
        assertEquals(3, Mathf.clamp(5, 0, 3));
    }

    @Test public void roundToStepsDownByDefaultAndHalfUpWhenAsked() {
        assertEquals(2f, Mathf.roundTo(2.7f, 1f), 0f);
        assertEquals(3f, Mathf.roundTo(2.7f, 1f, false), 0f);
        assertEquals(2f, Mathf.roundTo(2.2f, 0.5f), 0f);
    }

    @Test public void roundPointMovesAwayFromZero() {
        assertEquals(3, Mathf.roundPoint(2.1f));
        assertEquals(-3, Mathf.roundPoint(-2.1f));
        assertEquals(2, Mathf.roundPoint(2.0f));
    }

    @Test public void signReportsDirection() {
        assertEquals(-1, Mathf.sign(-5f));
        assertEquals(1, Mathf.sign(5f));
        assertEquals(0, Mathf.sign(0f));
    }

    @Test public void distanceIsEuclidean() {
        assertEquals(5f, Mathf.distance(0, 0, 3, 4), 1e-4f);
    }

    @Test public void fractKeepsTheFractionalPart() {
        assertEquals(0.25f, Mathf.fract(1.25f), 1e-6f);
    }
}
