package com.keepers.photoorganiser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AnalysisGestureRoutingTest {
    @Test public void openAssessmentOwnsTheWholeScrollGesture() {
        assertFalse(AnalysisGestureRouting.handleAsPhotoGesture(true));
        assertTrue(AnalysisGestureRouting.handleAsPhotoGesture(false));
    }

    @Test public void openAssessmentRoutesHorizontalSwipesBetweenPhotos() {
        assertTrue(AnalysisGestureRouting.isHorizontalPageSwipe(-70, 12, 64));
        assertTrue(AnalysisGestureRouting.isHorizontalPageSwipe(70, -12, 64));
        assertFalse(AnalysisGestureRouting.isHorizontalPageSwipe(12, 70, 64));
        assertFalse(AnalysisGestureRouting.isHorizontalPageSwipe(40, 2, 64));
    }
}
