package com.keepers.photoorganiser;

public record PhotoInsight(double quality, PhotoStackPosition stack,
        boolean recommended, String reason, PhotoAssessment assessment) {}
