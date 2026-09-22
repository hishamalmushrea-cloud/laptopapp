package com.winlator.math;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

/** Affine-transform coverage for the renderer's 2D math. */
public class XFormTest {
    private static void assertPoint(float[] xf, float x, float y, float ex, float ey) {
        assertArrayEquals(new float[]{ex, ey}, XForm.transformPoint(xf, x, y), 1e-4f);
    }

    @Test public void identityLeavesPointsUntouched() {
        assertPoint(XForm.getInstance(), 3, 4, 3, 4);
    }

    @Test public void translationOffsetsThePoint() {
        float[] xf = XForm.makeTranslation(XForm.getInstance(), 5, 6);
        assertPoint(xf, 1, 1, 6, 7);
    }

    @Test public void scaleMultipliesThePoint() {
        float[] xf = XForm.makeScale(XForm.getInstance(), 2, 3);
        assertPoint(xf, 2, 2, 4, 6);
    }

    @Test public void zeroRotationIsIdentity() {
        float[] xf = XForm.makeRotation(XForm.getInstance(), 0f);
        assertPoint(xf, 3, 4, 3, 4);
    }

    @Test public void translateThenScaleActsInLocalCoordinates() {
        // Matches production semantics: each op post-multiplies, so the new transform is
        // applied in the current (already transformed) coordinate space.
        float[] xf = XForm.getInstance();
        XForm.translate(xf, 10, 20);
        XForm.scale(xf, 2, 2);
        assertPoint(xf, 1, 1, 22, 42);
    }
}
