package com.keepers.photoorganiser;

import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class RecommendationPreferenceProfileTest {
    @Test public void repeatedFeedbackGraduallyFavoursTheTraitsInLovedPhotos() {
        PhotoFeatures sharp = feature("sharp", .95, .20);
        PhotoFeatures composed = feature("composed", .35, .95);
        RecommendationPreferenceProfile profile = RecommendationPreferenceProfile.learn(List.of(
                RecommendationFeedback.from(sharp, RecommendationFeedback.LOVED, ""),
                RecommendationFeedback.from(composed, RecommendationFeedback.NOT_FOR_ME,
                        "Too carefully posed")));

        assertTrue(profile.score(sharp) > profile.score(composed));
        assertTrue(profile.feedbackCount() == 2);
    }

    @Test public void noFeedbackPreservesTheExistingQualityRanking() {
        RecommendationPreferenceProfile profile = RecommendationPreferenceProfile.learn(List.of());

        PhotoFeatures better = new PhotoFeatures("better", 0, 0, .8,
                .8, .5, .2, .8, 0, -1, -1);
        PhotoFeatures worse = new PhotoFeatures("worse", 0, 0, .4,
                .4, .5, .9, .4, 0, -1, -1);
        assertTrue(profile.score(better) > profile.score(worse));
    }

    private static PhotoFeatures feature(String id, double focus, double composition) {
        return new PhotoFeatures(id, 0, id.hashCode(), (focus + composition) / 2,
                focus, .5, composition, focus, 0, -1, -1);
    }
}
