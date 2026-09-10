package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class QuickReviewCardTransformTest {
    @Test public void cardTracksAndRotatesWithDragWhileNextCardComesForward() {
        QuickReviewCardTransform right = QuickReviewCardTransform.from(120, 400);
        QuickReviewCardTransform left = QuickReviewCardTransform.from(-120, 400);

        assertEquals(120, right.translationX(), .001);
        assertTrue(right.rotation() > 0);
        assertTrue(left.rotation() < 0);
        assertTrue(right.nextScale() > .94f);
        assertTrue(right.nextScale() <= 1f);
        assertTrue(right.nextAlpha() > .72f);
    }

    @Test public void rotationAndProgressAreClampedForLongDrags() {
        QuickReviewCardTransform transform = QuickReviewCardTransform.from(2_000, 400);

        assertEquals(11f, transform.rotation(), .001);
        assertEquals(1f, transform.nextScale(), .001);
        assertEquals(1f, transform.nextAlpha(), .001);
    }
}
