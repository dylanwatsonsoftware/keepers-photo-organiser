package com.keepers.photoorganiser;

import java.util.Set;

public record BestShotResult(Set<String> recommended, Set<String> goodAlternatives) {
    public BestShotResult {
        recommended = Set.copyOf(recommended);
        goodAlternatives = Set.copyOf(goodAlternatives);
    }
}
