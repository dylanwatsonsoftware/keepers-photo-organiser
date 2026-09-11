package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
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

    @Test public void noFeedbackUsesTheBalancedAssessmentSignals() {
        RecommendationPreferenceProfile profile = RecommendationPreferenceProfile.learn(List.of());

        PhotoFeatures detailOnly = new PhotoFeatures("detail-only", 0, 0, .95,
                .95, .05, .05, .05, 0, -1, -1);
        PhotoFeatures balanced = new PhotoFeatures("balanced", 0, 0, .70,
                .80, .80, .80, .80, 0, -1, -1);

        assertTrue(profile.score(balanced) > profile.score(detailOnly));
    }

    @Test public void unavailableFaceSignalsDoNotLowerANonPortraitScore() {
        RecommendationPreferenceProfile profile = RecommendationPreferenceProfile.learn(List.of());
        PhotoFeatures landscape = new PhotoFeatures("landscape", 0, 0, .8,
                .8, .8, .8, .8, 0, -1, -1);

        assertEquals(.8, profile.score(landscape), .0001);
    }

    private static PhotoFeatures feature(String id, double focus, double composition) {
        return new PhotoFeatures(id, 0, id.hashCode(), (focus + composition) / 2,
                focus, .5, composition, focus, 0, -1, -1);
    }
}
