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
}
