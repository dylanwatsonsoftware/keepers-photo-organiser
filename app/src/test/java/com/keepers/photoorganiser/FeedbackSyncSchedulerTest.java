package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class FeedbackSyncSchedulerTest {
    @Test public void automaticSyncUsesAConservativeRegularInterval() {
        assertEquals(12, FeedbackSyncScheduler.REPEAT_INTERVAL);
        assertEquals(TimeUnit.HOURS, FeedbackSyncScheduler.REPEAT_INTERVAL_UNIT);
        assertEquals("recommendation-feedback-periodic", FeedbackSyncScheduler.PERIODIC_WORK_NAME);
    }
}
