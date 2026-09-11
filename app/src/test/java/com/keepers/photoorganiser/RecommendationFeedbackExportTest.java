package com.keepers.photoorganiser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class RecommendationFeedbackExportTest {
    @Test public void exportsUsefulSignalsAndCommentsWithoutPhotoIdentifiers() {
        PhotoFeatures photo = new PhotoFeatures("content://private/photo/123", 987654, 12345,
                .8, .9, .6, .7, .5, 2, .75, .95);

        String json = RecommendationFeedbackExport.toJson(List.of(
                RecommendationFeedback.from(photo, RecommendationFeedback.LOVED,
                        "Everyone looks happy")), "0.1-poc");

        assertTrue(json.contains("\"rating\":\"loved\""));
        assertTrue(json.contains("\"comment\":\"Everyone looks happy\""));
        assertTrue(json.contains("\"focus\":0.9"));
        assertTrue(json.contains("\"faceCount\":2"));
        assertFalse(json.contains("content://"));
        assertFalse(json.contains("987654"));
        assertFalse(json.contains("12345"));
    }

    @Test public void exportsAnonymousWithinStackComparisons() {
        PhotoFeatures keeper = new PhotoFeatures("content://private/keeper", 1, 2,
                .7, .8, .6, .9, .7, 1, .4, .95);
        PhotoFeatures alternative = new PhotoFeatures("content://private/alternative", 3, 4,
                .6, .7, .6, .5, .6, 1, .8, .9);

        String json = RecommendationFeedbackExport.toJson(List.of(), List.of(
                new StackPreferenceComparison(keeper, alternative)), "0.1-poc");

        assertTrue(json.contains("\"schemaVersion\":3"));
        assertTrue(json.contains("\"comparisons\":[{"));
        assertTrue(json.contains("\"preferredSignals\""));
        assertTrue(json.contains("\"alternativeSignals\""));
        assertFalse(json.contains("content://"));
    }

    @Test public void exportsAnonymousHiddenPhotoSignals() {
        PhotoFeatures hidden = new PhotoFeatures("content://private/hidden", 6, 7,
                .4, .5, .6, .7, .8, 0, -1, -1);

        String json = RecommendationFeedbackExport.toJson(
                List.of(RecommendationFeedback.from(hidden,
                        RecommendationFeedback.LOVED, "Old choice")),
                List.of(new StackPreferenceComparison(hidden,
                        new PhotoFeatures("alternative", 0, 0, .5))),
                List.of(hidden), "0.1-poc");

        assertTrue(json.contains("\"schemaVersion\":3"));
        assertTrue(json.contains("\"hiddenSignals\":[{"));
        assertTrue(json.contains("\"focus\":0.5"));
        assertFalse(json.contains("Old choice"));
        assertFalse(json.contains("preferredSignals"));
        assertFalse(json.contains("content://"));
    }
}
