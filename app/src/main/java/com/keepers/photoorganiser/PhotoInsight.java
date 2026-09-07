package com.keepers.photoorganiser;

public record PhotoInsight(double quality, PhotoStackPosition stack,
        boolean recommended, boolean goodAlternative, String reason,
        PhotoAssessment assessment) {}
