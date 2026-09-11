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
public class RecommendationFeedbackSnapshotFactoryTest {
    @Test public void buildsAnAnonymousVersionedSnapshotFromLocalEvidence() {
        Context context = RuntimeEnvironment.getApplication();
        new RecommendationFeedbackStore(context).clear();
        new RecommendationExportIdentityStore(context).clear();
        new RecommendationFeedbackStore(context).save(RecommendationFeedback.from(
                new PhotoFeatures("content://private/photo", 123, 456, .8),
                RecommendationFeedback.LOVED, "Great expression"));

        RecommendationFeedbackSnapshot snapshot =
                RecommendationFeedbackSnapshotFactory.create(context);

        assertEquals(1, snapshot.evidenceCount());
        assertEquals(1, snapshot.snapshotSequence());
        assertTrue(snapshot.json().contains("Great expression"));
        assertTrue(snapshot.json().contains(snapshot.sourceId()));
        assertFalse(snapshot.json().contains("content://private/photo"));
        assertFalse(snapshot.json().contains("123"));
        assertFalse(snapshot.json().contains("456"));
    }
}
