package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AlbumAutomationProgressTest {
    @Test public void describesQueuePositionDestinationAndCurrentStep() {
        AlbumAutomationProgress progress = AlbumAutomationProgress.from(
                2, 10, "Ada Photos", KeepersAccessibilityService.PHASE_FIND_OR_SEARCH);

        assertEquals("Keepers · 3 of 10", progress.title());
        assertEquals("Finding album · Ada Photos", progress.detail());
        assertEquals(3, progress.position());
        assertEquals(10, progress.total());
    }
}
