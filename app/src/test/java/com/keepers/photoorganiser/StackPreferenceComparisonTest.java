package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class StackPreferenceComparisonTest {
    @Test public void keeperIsComparedWithEveryNonKeeperInItsStack() {
        PhotoFeatures keeper = feature("keeper", .4, .9);
        PhotoFeatures alternativeA = feature("alternative-a", .9, .2);
        PhotoFeatures alternativeB = feature("alternative-b", .8, .3);
        List<String> stack = List.of("keeper", "alternative-a", "alternative-b");

        assertEquals(List.of(
                new StackPreferenceComparison(keeper, alternativeA, .5),
                new StackPreferenceComparison(keeper, alternativeB, .5)),
                StackPreferenceComparison.from(
                        List.of(keeper, alternativeA, alternativeB),
                        Map.of("keeper", stack, "alternative-a", stack,
                                "alternative-b", stack),
                        Set.of("keeper")));
    }

    @Test public void stackWithoutAKeeperProducesNoImplicitRejections() {
        PhotoFeatures first = feature("first", .4, .9);
        PhotoFeatures second = feature("second", .9, .2);
        List<String> stack = List.of("first", "second");

        assertEquals(List.of(), StackPreferenceComparison.from(List.of(first, second),
                Map.of("first", stack, "second", stack), Set.of()));
    }

    @Test public void everyPairSharesOneUnitOfEvidenceForItsWholeStack() {
        PhotoFeatures keeperA = feature("keeper-a", .4, .9);
        PhotoFeatures keeperB = feature("keeper-b", .5, .8);
        PhotoFeatures alternativeA = feature("alternative-a", .9, .2);
        PhotoFeatures alternativeB = feature("alternative-b", .8, .3);
        List<PhotoFeatures> features = List.of(
                keeperA, keeperB, alternativeA, alternativeB);
        List<String> stack = features.stream().map(PhotoFeatures::id).toList();

        List<StackPreferenceComparison> comparisons = StackPreferenceComparison.from(features,
                Map.of("keeper-a", stack, "keeper-b", stack,
                        "alternative-a", stack, "alternative-b", stack),
                Set.of("keeper-a", "keeper-b"));

        assertEquals(4, comparisons.size());
        assertEquals(1.0, comparisons.stream()
                .mapToDouble(StackPreferenceComparison::weight).sum(), .0001);
    }

    private static PhotoFeatures feature(String id, double focus, double composition) {
        return new PhotoFeatures(id, 0, id.hashCode(), .6,
                focus, .6, composition, .6, 0, -1, -1);
    }
}
