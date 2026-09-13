package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class RecommendationPreferenceProfileTest {
    @Test public void absoluteFeedbackDoesNotRewriteTechnicalQualityPreferences() {
        PhotoFeatures sharp = feature("sharp", .95, .20);
        PhotoFeatures composed = feature("composed", .35, .95);
        RecommendationPreferenceProfile profile = RecommendationPreferenceProfile.learn(List.of(
                RecommendationFeedback.from(sharp, RecommendationFeedback.LOVED, ""),
                RecommendationFeedback.from(composed, RecommendationFeedback.NOT_FOR_ME,
                        "Too carefully posed")));

        RecommendationPreferenceProfile baseline = RecommendationPreferenceProfile.learn(List.of());
        assertEquals(baseline.score(sharp), profile.score(sharp), .0001);
        assertEquals(0, profile.feedbackCount());
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

    @Test public void contextChangesWhichTechnicalSignalsMatterMost() {
        PhotoFeatures focusLed = new PhotoFeatures("focus-led", 0, 0, .7,
                1, .7, .4, .7, 0, -1, -1, -1);
        PhotoFeatures compositionLed = new PhotoFeatures("composition-led", 0, 0, .7,
                .4, .7, 1, .7, 0, -1, -1, -1);
        RecommendationPreferenceProfile landscape = RecommendationPreferenceProfile
                .learn(List.of()).withContexts(Map.of(
                        "focus-led", PhotoContext.of(PhotoContextType.LANDSCAPE, 1),
                        "composition-led", PhotoContext.of(PhotoContextType.LANDSCAPE, 1)));
        RecommendationPreferenceProfile document = RecommendationPreferenceProfile
                .learn(List.of()).withContexts(Map.of(
                        "focus-led", PhotoContext.of(PhotoContextType.DOCUMENT, 1),
                        "composition-led", PhotoContext.of(PhotoContextType.DOCUMENT, 1)));

        assertTrue(landscape.score(compositionLed) > landscape.score(focusLed));
        assertTrue(document.score(focusLed) > document.score(compositionLed));
    }

    @Test public void smileDoesNotActAsUniversalPhotoQuality() {
        RecommendationPreferenceProfile profile = RecommendationPreferenceProfile.learn(List.of());
        PhotoFeatures smiling = portrait("smiling", .8, 1, .8);
        PhotoFeatures candid = portrait("candid", .8, 0, .8);

        assertEquals(profile.score(candid), profile.score(smiling), .0001);
    }

    @Test public void openEyesReceiveExtraWeightWhenComparingPortraits() {
        RecommendationPreferenceProfile profile = RecommendationPreferenceProfile.learn(List.of());
        PhotoFeatures openEyes = portrait("open", .4, .5, 1);
        PhotoFeatures sharperClosedEyes = portrait("closed", 1, .5, .5);

        assertTrue(profile.score(openEyes) > profile.score(sharperClosedEyes));
    }

    @Test public void facingTheCameraBreaksAPortraitTie() {
        RecommendationPreferenceProfile profile = RecommendationPreferenceProfile.learn(List.of());
        PhotoFeatures facing = portrait("facing", .8, .5, .8);
        PhotoFeatures lookingAway = new PhotoFeatures("away", 0, 0, .8,
                .8, .8, .8, .8, 1, .5, .8, .1);

        assertTrue(profile.score(facing) > profile.score(lookingAway));
    }

    @Test public void withinStackComparisonsTeachWhichTraitsWon() {
        PhotoFeatures preferred = comparisonFeature("preferred", .3, .95);
        PhotoFeatures alternative = comparisonFeature("alternative", .95, .3);
        RecommendationPreferenceProfile profile = RecommendationPreferenceProfile.learn(
                List.of(), List.of(new StackPreferenceComparison(preferred, alternative)));

        assertTrue(profile.score(comparisonFeature("similar-to-preferred", .35, .9))
                > profile.score(comparisonFeature("similar-to-alternative", .9, .35)));
        assertEquals(1, profile.feedbackCount());
    }

    @Test public void hiddenPhotosAreCapturedWithoutRewritingTechnicalQuality() {
        PhotoFeatures hidden = comparisonFeature("hidden", .95, .3);
        RecommendationPreferenceProfile profile = RecommendationPreferenceProfile.learn(
                List.of(RecommendationFeedback.from(hidden, RecommendationFeedback.LOVED, "")),
                List.of(new StackPreferenceComparison(
                        hidden,
                        comparisonFeature("stack-alternative", .3, .95))),
                List.of(hidden));

        RecommendationPreferenceProfile baseline = RecommendationPreferenceProfile.learn(List.of());
        PhotoFeatures candidate = comparisonFeature("candidate", .9, .35);
        assertEquals(baseline.score(candidate), profile.score(candidate), .0001);
        assertEquals(0, profile.feedbackCount());
    }

    @Test public void eachStackHasEqualLearningInfluenceRegardlessOfItsSize() {
        PhotoFeatures composed = comparisonFeature("composed", .3, .9);
        PhotoFeatures sharp = comparisonFeature("sharp", .9, .3);
        RecommendationPreferenceProfile profile = RecommendationPreferenceProfile.learn(List.of(),
                List.of(
                        new StackPreferenceComparison(composed,
                                comparisonFeature("sharp-a", .9, .3), 1.0 / 3),
                        new StackPreferenceComparison(composed,
                                comparisonFeature("sharp-b", .9, .3), 1.0 / 3),
                        new StackPreferenceComparison(composed,
                                comparisonFeature("sharp-c", .9, .3), 1.0 / 3),
                        new StackPreferenceComparison(sharp,
                                comparisonFeature("composed-a", .3, .9))));

        assertEquals(profile.score(comparisonFeature("focus", .9, .3)),
                profile.score(comparisonFeature("composition", .3, .9)), .0001);
        assertEquals(2, profile.feedbackCount());
    }

    @Test public void multipleKeepersInOneStackStillCountAsOneChoice() {
        PhotoFeatures keeperA = comparisonFeature("keeper-a", .4, .9);
        PhotoFeatures keeperB = comparisonFeature("keeper-b", .5, .8);
        PhotoFeatures alternativeA = comparisonFeature("alternative-a", .9, .2);
        PhotoFeatures alternativeB = comparisonFeature("alternative-b", .8, .3);
        List<PhotoFeatures> features = List.of(
                keeperA, keeperB, alternativeA, alternativeB);
        List<String> stack = features.stream().map(PhotoFeatures::id).toList();

        RecommendationPreferenceProfile profile = RecommendationPreferenceProfile.learn(List.of(),
                StackPreferenceComparison.from(features,
                        Map.of("keeper-a", stack, "keeper-b", stack,
                                "alternative-a", stack, "alternative-b", stack),
                        Set.of("keeper-a", "keeper-b")));

        assertEquals(1, profile.feedbackCount());
    }

    private static PhotoFeatures feature(String id, double focus, double composition) {
        return new PhotoFeatures(id, 0, id.hashCode(), (focus + composition) / 2,
                focus, .5, composition, focus, 0, -1, -1);
    }

    private static PhotoFeatures comparisonFeature(String id, double focus, double composition) {
        return new PhotoFeatures(id, 0, id.hashCode(), .6,
                focus, .6, composition, .6, 0, -1, -1);
    }

    private static PhotoFeatures portrait(String id, double focus, double smile, double eyesOpen) {
        return new PhotoFeatures(id, 0, 0, .8, focus, .8, .8, .8,
                1, smile, eyesOpen, .9);
    }
}
