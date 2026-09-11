package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class FeedbackSyncPreferencesTest {
    @Test public void automaticSharingIsOptInAndRemembersItsLastResult() {
        Context context = RuntimeEnvironment.getApplication();
        FeedbackSyncPreferences preferences = new FeedbackSyncPreferences(context);
        preferences.clear();

        assertFalse(preferences.isEnabled());

        preferences.setEnabled(true);
        preferences.recordSuccess(8, 1234);

        assertTrue(preferences.isEnabled());
        assertEquals(8, preferences.lastSnapshotSequence());
        assertEquals(1234, preferences.lastSuccessAtMillis());
        assertEquals("", preferences.lastError());

        preferences.recordFailure("Network unavailable");
        assertEquals("Network unavailable", preferences.lastError());
    }
}
