package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.os.Handler;
import android.os.Looper;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

@RunWith(RobolectricTestRunner.class)
public class AlbumStepRetrySchedulerTest {
    @Test public void armedAutomationRetriesOnceAfterControlsHaveTimeToAppear() {
        AtomicInteger attempts = new AtomicInteger();
        AlbumStepRetryScheduler scheduler = new AlbumStepRetryScheduler(
                new Handler(Looper.getMainLooper()), attempts::incrementAndGet);

        scheduler.ensureScheduled(true);
        scheduler.ensureScheduled(true);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(599));
        assertEquals(0, attempts.get());
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1));
        assertEquals(1, attempts.get());
    }

    @Test public void disarmedAutomationDoesNotRetry() {
        AtomicInteger attempts = new AtomicInteger();
        AlbumStepRetryScheduler scheduler = new AlbumStepRetryScheduler(
                new Handler(Looper.getMainLooper()), attempts::incrementAndGet);

        scheduler.ensureScheduled(true);
        scheduler.cancel();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));

        assertEquals(0, attempts.get());
    }
}
