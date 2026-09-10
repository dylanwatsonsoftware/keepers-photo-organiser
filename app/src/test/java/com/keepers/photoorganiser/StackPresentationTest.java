package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class StackPresentationTest {
    @Test public void galleryShowsOnlyRecommendedCoverForEachStack() {
        assertEquals(List.of("stack-best", "single"), StackPresentation.visibleIds(
                List.of("stack-first", "stack-best", "stack-last", "single"),
                Map.of(
                        "stack-first", List.of("stack-first", "stack-best", "stack-last"),
                        "stack-best", List.of("stack-first", "stack-best", "stack-last"),
                        "stack-last", List.of("stack-first", "stack-best", "stack-last")),
                Set.of("stack-best"), Set.of()));
    }

    @Test public void galleryPrefersAKeeperAsTheStackCover() {
        List<String> stack = List.of("stack-first", "stack-best", "stack-keeper");

        assertEquals(List.of("stack-keeper", "single"), StackPresentation.visibleIds(
                List.of("stack-first", "stack-best", "stack-keeper", "single"),
                Map.of("stack-first", stack, "stack-best", stack, "stack-keeper", stack),
                Set.of("stack-best"), Set.of("stack-keeper")));
    }

    @Test public void navigationKeepsAnOpenedStackMemberButOmitsItsSiblings() {
        List<String> stack = List.of("stack-first", "stack-opened", "stack-last");

        assertEquals(List.of("stack-opened", "single"), StackPresentation.navigationIds(
                List.of("stack-first", "stack-opened", "stack-last", "single"),
                Map.of("stack-first", stack, "stack-opened", stack, "stack-last", stack),
                Set.of("stack-first"), Set.of(), "stack-opened"));
    }

    @Test public void overlappingStacksNeverShowTheSameCoverTwice() {
        assertEquals(List.of("shared"), StackPresentation.visibleIds(
                List.of("first-stack", "second-stack"),
                Map.of(
                        "first-stack", List.of("shared", "first-stack"),
                        "second-stack", List.of("shared", "second-stack")),
                Set.of(), Set.of()));
    }
}
