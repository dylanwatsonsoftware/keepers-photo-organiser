package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PhotoInsightStoreTest {
    @Test public void storesQualityStackAndRecommendationReasonForReview() {
        PhotoInsightStore store = new PhotoInsightStore(RuntimeEnvironment.getApplication());
        store.save(List.of(new PhotoFeatures("photo-a", 1, 0, 0.084,
                        0.8, 0.7, 0.6, 0.9)),
                Map.of("photo-a", new PhotoStackPosition(2, 6)), Set.of("photo-a"));

        PhotoInsight insight = store.load("photo-a");

        assertEquals(0.084, insight.quality(), 0.0001);
        assertEquals(new PhotoStackPosition(2, 6), insight.stack());
        assertEquals(true, insight.recommended());
        assertEquals("Best detail score in this stack", insight.reason());
        assertEquals(62, insight.assessment().score());
        assertTrue(insight.assessment().explanation().contains("Motion blur — 90%"));
    }

    @Test public void explainsStrongDuplicateAsAGoodAlternative() {
        PhotoInsightStore store = new PhotoInsightStore(RuntimeEnvironment.getApplication());
        store.save(List.of(new PhotoFeatures("photo-b", 1, 0, 0.7)), Map.of(), Set.of(),
                Set.of("photo-b"));

        PhotoInsight insight = store.load("photo-b");

        assertEquals(true, insight.goodAlternative());
        assertEquals("A near-identical photo ranked slightly higher", insight.reason());
    }
}
