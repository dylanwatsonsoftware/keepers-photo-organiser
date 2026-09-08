package com.keepers.photoorganiser;

public final class AssessmentStatusStyle {
    private AssessmentStatusStyle() {}

    public static int iconRes(boolean recommended, boolean goodAlternative) {
        return recommended ? R.drawable.ic_star
                : goodAlternative ? R.drawable.ic_star_outline : 0;
    }
}
