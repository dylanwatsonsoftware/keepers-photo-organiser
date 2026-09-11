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
                new StackPreferenceComparison(keeper, alternativeA),
                new StackPreferenceComparison(keeper, alternativeB)),
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

    private static PhotoFeatures feature(String id, double focus, double composition) {
        return new PhotoFeatures(id, 0, id.hashCode(), .6,
                focus, .6, composition, .6, 0, -1, -1);
    }
}
