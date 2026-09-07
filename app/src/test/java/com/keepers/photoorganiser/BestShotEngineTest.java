package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.Test;

public class BestShotEngineTest {
    @Test public void recommendsSharperPhotoFromNearbyVisualDuplicatePair() {
        PhotoFeatures softer = feature("soft", 1_000, 0b101010L, 0.35);
        PhotoFeatures sharper = feature("sharp", 20_000, 0b101011L, 0.82);

        assertEquals(Set.of("sharp"), BestShotEngine.recommend(List.of(softer, sharper)));
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

    private static PhotoFeatures feature(String id, long takenAt, long hash, double quality) {
        return new PhotoFeatures(id, takenAt, hash, quality);
    }
}
