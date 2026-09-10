package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class BestShotEngineTest {
    @Test public void recommendsSharperPhotoFromNearbyVisualDuplicatePair() {
        PhotoFeatures softer = feature("soft", 1_000, 0b101010L, 0.35);
        PhotoFeatures sharper = feature("sharp", 20_000, 0b101011L, 0.82);

        assertEquals(Set.of("sharp"), BestShotEngine.recommend(List.of(softer, sharper)));
    }

    @Test public void learnedPreferencesCanChangeTheBestPhotoWithinAStack() {
        PhotoFeatures technical = new PhotoFeatures("technical", 1_000, 0L, .80,
                .9, .5, .2, .9, 0, -1, -1);
        PhotoFeatures composed = new PhotoFeatures("composed", 2_000, 1L, .70,
                .4, .5, .95, .4, 0, -1, -1);
        RecommendationPreferenceProfile profile = RecommendationPreferenceProfile.learn(List.of(
                RecommendationFeedback.from(composed, RecommendationFeedback.LOVED, "Framing"),
                RecommendationFeedback.from(technical, RecommendationFeedback.NOT_FOR_ME, "Framing"),
                RecommendationFeedback.from(new PhotoFeatures("liked-2", 3, 3, .7,
                        .4, .5, 1, .4, 0, -1, -1), RecommendationFeedback.LOVED, "Framing"),
                RecommendationFeedback.from(new PhotoFeatures("no-2", 4, 4, .8,
                        .9, .5, .1, .9, 0, -1, -1), RecommendationFeedback.NOT_FOR_ME, "Framing")));

        assertEquals(Set.of("composed"), BestShotEngine.classify(
                List.of(technical, composed), profile).recommended());
    }

    @Test public void recommendsBestPhotoFromNearbySceneEvenWhenCompositionChanges() {
        PhotoFeatures first = feature("first", 1_000, 0L, 0.4);
        PhotoFeatures different = feature("different", 20_000, -1L, 0.9);

        assertEquals(Set.of("different"), BestShotEngine.recommend(List.of(first, different)));
    }

    @Test public void doesNotGroupSimilarPhotosFromDifferentScenesInTime() {
        PhotoFeatures first = feature("first", 1_000, 7L, 0.4);
        PhotoFeatures later = feature("later", 301_000, 7L, 0.9);

        assertEquals(Set.of("later"), BestShotEngine.recommend(List.of(first, later)));
    }

    @Test public void recommendsOneBestPhotoFromAThreeShotSequence() {
        assertEquals(Set.of("middle"), BestShotEngine.recommend(List.of(
                feature("first", 1_000, 8L, 0.5),
                feature("middle", 5_000, 9L, 0.9),
                feature("last", 9_000, 10L, 0.7))));
    }

    @Test public void recommendsOnlyTopThirdOfDistinctCandidates() {
        assertEquals(Set.of("best", "second"), BestShotEngine.recommend(List.of(
                feature("low", 1, 0L, 0.1),
                feature("best", 2, -1L, 0.9),
                feature("third", 3, 0xAAAAAAAAAAAAAAAAL, 0.7),
                feature("second", 4, 0x5555555555555555L, 0.8))));
    }

    @Test public void numbersOnlyDuplicateStacksDeterministically() {
        Map<String, PhotoStackPosition> stacks = BestShotEngine.stacks(List.of(
                feature("second-b", 200_040, -2L, 0.4),
                feature("unique", 500_000, 0xAAAAAAAAAAAAAAAAL, 0.8),
                feature("first-b", 20, 0b00000001L, 0.5),
                feature("second-a", 200_000, -1L, 0.6),
                feature("first-a", 1, 0L, 0.7)));

        assertEquals(Map.of(
                "first-a", new PhotoStackPosition(1, 2),
                "first-b", new PhotoStackPosition(2, 2),
                "second-a", new PhotoStackPosition(1, 2),
                "second-b", new PhotoStackPosition(2, 2)), stacks);
    }

    @Test public void stacksRapidSequenceDespiteCompositionAndExposureChanges() {
        assertEquals(Set.of("first", "second", "third"), BestShotEngine.stacks(List.of(
                feature("first", 1_000, 0L, 0.4),
                feature("second", 35_000, 0xFFFFL, 0.9),
                feature("third", 90_000, 0xFFFFFL, 0.7))).keySet());
    }

    @Test public void splitsRapidSequenceAtLargeVisualSceneChange() {
        assertEquals(Map.of(
                "inside-a", new PhotoStackPosition(1, 2),
                "inside-b", new PhotoStackPosition(2, 2),
                "entrance-a", new PhotoStackPosition(1, 2),
                "entrance-b", new PhotoStackPosition(2, 2)), BestShotEngine.stacks(List.of(
                feature("inside-a", 1_000, 0L, 0.4),
                feature("inside-b", 20_000, 0xFFFFL, 0.8),
                feature("entrance-a", 30_000, -1L, 0.7),
                feature("entrance-b", 40_000, -256L, 0.6))));
    }

    @Test public void groupsSameNamedFacesWithinAMinuteDespiteFramingChanges() {
        List<PhotoFeatures> sequence = List.of(
                feature("wide", 1_000, 0L, .5),
                feature("closer", 25_000, -1L, .8),
                feature("portrait", 55_000, 0xAAAAAAAAAAAAAAAAL, .7));
        Map<String, Set<String>> namedFaces = Map.of(
                "wide", Set.of("child-a", "child-b"),
                "closer", Set.of("child-a", "child-b"),
                "portrait", Set.of("child-a", "child-b"));

        assertEquals(Set.of("wide", "closer", "portrait"),
                BestShotEngine.stacks(sequence, namedFaces).keySet());
        assertEquals(Set.of("closer"),
                BestShotEngine.classify(sequence, namedFaces).recommended());
    }

    @Test public void distinctInterveningPhotoStillBreaksNamedFaceSequence() {
        List<PhotoFeatures> sequence = List.of(
                feature("children-a", 1_000, 0L, .5),
                feature("noticeboard", 20_000, -1L, .9),
                feature("children-b", 40_000, 0xAAAAAAAAAAAAAAAAL, .8));
        Map<String, Set<String>> namedFaces = Map.of(
                "children-a", Set.of("child-a", "child-b"),
                "children-b", Set.of("child-a", "child-b"));

        assertEquals(Map.of(), BestShotEngine.stacks(sequence, namedFaces));
    }

    @Test public void recommendsOnlyOneBestPhotoFromOneDisplayedStack() {
        assertEquals(Set.of("best"), BestShotEngine.recommend(List.of(
                feature("one", 1_000, 0L, 0.4),
                feature("two", 10_000, 0xFFFL, 0.5),
                feature("three", 20_000, 0xFFFFFFL, 0.6),
                feature("best", 30_000, 0xFFFFFFFFFL, 0.9),
                feature("five", 40_000, 0xFFFFFFFFFFFFL, 0.7),
                feature("six", 50_000, 0x0FFFFFFFFFFFFFFFL, 0.8))));
    }

    @Test public void goodStackContributesItsBestShotOutsideTheGlobalCutoff() {
        assertEquals(Set.of("overall-best", "runner-up", "stack-best"),
                BestShotEngine.recommend(List.of(
                        feature("stack-soft", 1_000, 0L, 0.35),
                        feature("stack-best", 2_000, 1L, 0.45),
                        feature("overall-best", 200_000, -1L, 0.9),
                        feature("runner-up", 400_000, 0xAAAAAAAAAAAAAAAAL, 0.8),
                        feature("third", 600_000, 0x5555555555555555L, 0.7))));
    }

    @Test public void unusableStackDoesNotEarnARecommendationFromAttemptsAlone() {
        assertEquals(Set.of("overall-best", "runner-up"), BestShotEngine.recommend(List.of(
                feature("stack-blurrier", 1_000, 0L, 0.01),
                feature("stack-blurry", 2_000, 1L, 0.02),
                feature("overall-best", 200_000, -1L, 0.9),
                feature("runner-up", 400_000, 0xAAAAAAAAAAAAAAAAL, 0.8),
                feature("third", 600_000, 0x5555555555555555L, 0.7))));
    }

    @Test public void identicalCandidatesProduceOneRecommendationAndGoodAlternative() {
        BestShotResult result = BestShotEngine.classify(List.of(
                feature("better", 1_000, 7L, 0.9),
                feature("almost-as-good", 301_000, 7L, 0.8),
                feature("different", 600_000, -1L, 0.2)));

        assertEquals(Set.of("better"), result.recommended());
        assertEquals(Set.of("almost-as-good"), result.goodAlternatives());
    }

    @Test public void exposesOrderedMembersForEveryPhotoInAStack() {
        Map<String, List<String>> members = BestShotEngine.stackMembers(List.of(
                feature("first", 1_000, 1L, 0.5),
                feature("second", 2_000, 2L, 0.8),
                feature("separate", 300_000, -1L, 0.9)));

        assertEquals(List.of("first", "second"), members.get("first"));
        assertEquals(List.of("first", "second"), members.get("second"));
        assertEquals(null, members.get("separate"));
    }

    private static PhotoFeatures feature(String id, long takenAt, long hash, double quality) {
        return new PhotoFeatures(id, takenAt, hash, quality);
    }
}
