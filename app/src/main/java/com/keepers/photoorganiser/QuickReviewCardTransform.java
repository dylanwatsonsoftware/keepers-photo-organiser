package com.keepers.photoorganiser;

public record QuickReviewCardTransform(float translationX, float translationY, float rotation,
        float nextScale, float nextAlpha, float nextTranslationY) {
    public static QuickReviewCardTransform from(float dragX, float width) {
        float safeWidth = Math.max(1, width);
        float progress = Math.min(1, Math.abs(dragX) / (safeWidth * .45f));
        float rotation = Math.max(-11, Math.min(11, dragX / safeWidth * 18));
        return new QuickReviewCardTransform(dragX, Math.abs(dragX) * .025f, rotation,
                .96f + .04f * progress, .72f + .28f * progress,
                24f * (1 - progress));
    }
}
