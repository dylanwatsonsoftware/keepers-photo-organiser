package com.keepers.photoorganiser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class RecommendationFeedbackExportTest {
    @Test public void exportsUsefulSignalsAndCommentsWithoutPhotoIdentifiers() {
        PhotoFeatures photo = new PhotoFeatures("content://private/photo/123", 987654, 12345,
                .8, .9, .6, .7, .5, 2, .75, .95, .72);

        String json = RecommendationFeedbackExport.toJson(List.of(
                RecommendationFeedback.from(photo, RecommendationFeedback.LOVED,
                        "Everyone looks happy")), "0.1-poc");

        assertTrue(json.contains("\"rating\":\"loved\""));
        assertTrue(json.contains("\"comment\":\"Everyone looks happy\""));
        assertTrue(json.contains("\"focus\":0.9"));
        assertTrue(json.contains("\"faceCount\":2"));
        assertTrue(json.contains("\"cameraFacing\":0.72"));
        assertTrue(json.contains("\"schemaVersion\":5"));
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

        assertTrue(json.contains("\"schemaVersion\":5"));
        assertTrue(json.contains("\"comparisons\":[{"));
        assertTrue(json.contains("\"preferredSignals\""));
        assertTrue(json.contains("\"alternativeSignals\""));
        assertFalse(json.contains("content://"));
    }

    @Test public void exportedComparisonsShareOneUnitOfWeightPerStack() {
        PhotoFeatures keeper = new PhotoFeatures("keeper", 1, 2, .7);

        String json = RecommendationFeedbackExport.toJson(List.of(), List.of(
                new StackPreferenceComparison(keeper,
                        new PhotoFeatures("alternative-a", 3, 4, .6), .5),
                new StackPreferenceComparison(keeper,
                        new PhotoFeatures("alternative-b", 5, 6, .5), .5)), "0.1-poc");

        assertTrue(json.contains("\"weight\":0.5"));
        assertTrue(json.indexOf("\"weight\":0.5")
                != json.lastIndexOf("\"weight\":0.5"));
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

        assertTrue(json.contains("\"schemaVersion\":5"));
        assertTrue(json.contains("\"hiddenSignals\":[{"));
        assertTrue(json.contains("\"focus\":0.5"));
        assertFalse(json.contains("Old choice"));
        assertFalse(json.contains("preferredSignals"));
        assertFalse(json.contains("content://"));
    }

    @Test public void identifiesReplaceableSnapshotsAndTreatsMissingEvidenceAsUnknown() {
        String json = RecommendationFeedbackExport.toJson(List.of(), List.of(), List.of(),
                "0.1-poc", new RecommendationExportMetadata("anonymous-source", 7));

        assertTrue(json.contains("\"schemaVersion\":5"));
        assertTrue(json.contains("\"sourceId\":\"anonymous-source\""));
        assertTrue(json.contains("\"snapshotSequence\":7"));
        assertTrue(json.contains("\"mergePolicy\":\"latest_snapshot_per_source\""));
        assertTrue(json.contains("\"missingEvidence\":\"unknown\""));
        assertTrue(json.contains("\"coverage\":{\"feedback\":0,\"comparisons\":0,"
                + "\"hiddenSignals\":0}"));
    }
}
