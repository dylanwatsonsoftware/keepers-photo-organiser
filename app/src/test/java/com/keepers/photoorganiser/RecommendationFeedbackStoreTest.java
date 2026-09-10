package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class RecommendationFeedbackStoreTest {
    @Test public void savesRatingFeatureSnapshotAndFreeTextComment() {
        Context context = RuntimeEnvironment.getApplication();
        RecommendationFeedbackStore store = new RecommendationFeedbackStore(context);
        store.clear();
        PhotoFeatures photo = new PhotoFeatures("content://photo/1", 1, 2, .6,
                .7, .5, .8, .4, 1, .9, .8);

        store.save(RecommendationFeedback.from(photo, RecommendationFeedback.LOVED,
                "Great expression | worth framing"));

        List<RecommendationFeedback> loaded = store.load();
        assertEquals(1, loaded.size());
        assertEquals(RecommendationFeedback.LOVED, loaded.get(0).rating());
        assertEquals("Great expression | worth framing", loaded.get(0).comment());
        assertEquals(photo, loaded.get(0).features());
    }

    @Test public void newerFeedbackReplacesFeedbackForTheSamePhoto() {
        Context context = RuntimeEnvironment.getApplication();
        RecommendationFeedbackStore store = new RecommendationFeedbackStore(context);
        store.clear();
        PhotoFeatures photo = new PhotoFeatures("same", 1, 2, .6);
        store.save(RecommendationFeedback.from(photo, RecommendationFeedback.LOVED, "First"));

        store.save(RecommendationFeedback.from(photo, RecommendationFeedback.NOT_FOR_ME, "Blurred"));

        assertEquals(1, store.load().size());
        assertEquals(RecommendationFeedback.NOT_FOR_ME, store.load().get(0).rating());
        assertEquals("Blurred", store.load().get(0).comment());
    }
}
