package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class QuickReviewDecisionTest {
    @Test public void rightKeepsLeftRejectsAndSmallMovementDoesNothing() {
        assertEquals(QuickReviewDecision.KEEP, QuickReviewDecision.fromSwipe(80, 64));
        assertEquals(QuickReviewDecision.REJECT, QuickReviewDecision.fromSwipe(-80, 64));
        assertEquals(QuickReviewDecision.NONE, QuickReviewDecision.fromSwipe(40, 64));
    }
}
