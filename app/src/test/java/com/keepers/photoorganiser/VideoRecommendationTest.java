package com.keepers.photoorganiser;

import static org.junit.Assert.*;
import android.content.Context;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class VideoRecommendationTest {
    private VideoFeatures video(String id, double detail, double focus) {
        return new VideoFeatures(1, id, 12_345, 3, detail, focus, .8, .8, .8, 0, 0);
    }
    @Test public void persistsVideoFeedbackSeparatelyAndKeepsCommentsLocal() {
        Context context = RuntimeEnvironment.getApplication();
        VideoFeedbackStore store = new VideoFeedbackStore(context);
        store.clear();
        VideoFeedback feedback = new VideoFeedback(video("content://secret/clip", .9, .4),
                VideoFeedback.LOVED, "Private family comment");
        store.save(feedback);
        assertEquals(feedback, store.load(feedback.features().id()));
        assertNull(new RecommendationFeedbackStore(context).load(feedback.features().id()));
        String json = RecommendationFeedbackSnapshotFactory.create(context).json();
        assertTrue(json.contains("\"videoFeedback\":["));
        assertTrue(json.contains("\"mediaType\":\"video\""));
        assertTrue(json.contains("\"videoSchemaVersion\":1"));
        assertFalse(json.contains("content://secret"));
        assertFalse(json.contains("Private family"));
        assertFalse(json.contains("12345"));
        store.clear();
    }
    @Test public void frozenFramePenaltyIsExplainedWhenItDominates() {
        VideoFeatures frozen = new VideoFeatures(1, "frozen", 10000, 3, 1, 1, 1, 1, 1, 0, 1);
        assertTrue(VideoPreferenceProfile.learn(List.of()).ranking(frozen).explanation()
                .contains("Changing scene"));
    }
    @Test public void balancedVideoChoicesLearnFocusWithoutChangingPhotoProfile() {
        VideoFeatures clear = video("clear", .3, 1);
        VideoFeatures detailed = video("detailed", 1, .3);
        VideoPreferenceProfile baseline = VideoPreferenceProfile.learn(List.of());
        List<VideoFeedback> evidence = java.util.stream.IntStream.range(0, 20)
                .mapToObj(index -> new VideoFeedback(video("clip" + index,
                        index % 2 == 0 ? .3 : 1, index % 2 == 0 ? 1 : .3),
                        index % 2 == 0 ? VideoFeedback.LOVED : VideoFeedback.NOT_FOR_ME, ""))
                .toList();
        VideoPreferenceProfile learned = VideoPreferenceProfile.learn(evidence);
        assertEquals(clear.score(), baseline.ranking(clear).score());
        assertTrue(learned.score(clear) > learned.score(detailed));
        assertTrue(learned.ranking(detailed).explanation().contains("Focus"));
        assertTrue(learned.ranking(clear).explanation().contains("20"));
        VideoPreferenceProfile.Evaluation evaluation = VideoPreferenceProfile.evaluate(evidence);
        assertTrue(evaluation.heldOutCount() > 0);
        assertTrue(evaluation.learnedPairAccuracy() > evaluation.baselinePairAccuracy());
        assertEquals(0, RecommendationPreferenceProfile.learn(List.of()).feedbackCount());
    }
    @Test public void snapshotsCountHiddenVideosWithoutDuplicatingEarlierPositiveVotes() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        new RecommendationFeedbackStore(context).clear();
        VideoFeedbackStore store = new VideoFeedbackStore(context);
        store.clear();
        VideoFeatures clip = video("private-clip", .9, .2);
        new VideoInsightStore(context).save(clip);
        store.save(new VideoFeedback(clip, VideoFeedback.LOVED, "local note"));
        new HiddenPhotoStore(context).hide(java.util.Set.of(clip.id()));
        RecommendationFeedbackSnapshot snapshot = RecommendationFeedbackSnapshotFactory.create(context);
        org.json.JSONArray rows = new org.json.JSONObject(snapshot.json()).getJSONArray("videoFeedback");
        assertEquals(1, rows.length());
        assertEquals("hidden", rows.getJSONObject(0).getString("rating"));
        assertEquals(1, snapshot.evidenceCount());
        store.clear();
        new HiddenPhotoStore(context).clear();
    }
    @Test public void unsupportedSchemasAndNonFiniteSignalsCannotEnterExports() {
        assertThrows(IllegalArgumentException.class, () -> new VideoFeedback(
                new VideoFeatures(99, "future", 0, 3, .5, .5, .5, .5, .5, 0, 0), 1, ""));
        assertThrows(IllegalArgumentException.class, () -> new VideoFeedback(
                video("invalid", Double.NaN, .5), 1, ""));
    }
    @Test public void repeatedClipVotesDoNotInflateTrainingOrLeakAcrossTheEvaluationSplit() {
        VideoFeedback first = new VideoFeedback(video("same", .9, .2), 1, "");
        VideoFeedback latest = new VideoFeedback(video("same", .9, .2), -1, "");
        VideoPreferenceProfile.Evaluation evaluation = VideoPreferenceProfile.evaluate(List.of(first, latest));
        assertEquals(1, evaluation.trainingCount() + evaluation.heldOutCount());
        assertEquals(0, evaluation.pairCount());
        assertTrue(Double.isNaN(evaluation.learnedPairAccuracy()));
    }
    @Test public void hideOverridesLovedEvidenceAndOneSidedFeedbackDoesNotTrain() {
        VideoFeatures clip = video("clip", .9, .2);
        VideoPreferenceProfile oneSided = VideoPreferenceProfile.learn(List.of(
                new VideoFeedback(clip, VideoFeedback.LOVED, "")));
        assertEquals(clip.score() / 100d, oneSided.score(clip), .006);
        VideoFeedbackStore store = new VideoFeedbackStore(RuntimeEnvironment.getApplication());
        store.clear();
        store.save(new VideoFeedback(clip, VideoFeedback.LOVED, "keep comment"));
        store.record(clip, VideoFeedback.HIDDEN);
        assertEquals(VideoFeedback.HIDDEN, store.load("clip").rating());
        assertEquals("keep comment", store.load("clip").comment());
        store.clear();
    }
}
