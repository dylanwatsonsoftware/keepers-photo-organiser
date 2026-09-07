package com.keepers.photoorganiser;

import java.util.Locale;
import java.util.Set;

public final class FavouriteControlMatcher {
    private static final Set<String> SAFE_LABELS = Set.of(
            "favourite", "favorite", "add to favourites", "add to favorites");
    private FavouriteControlMatcher() {}

    public static boolean matches(CharSequence label) {
        return label != null && SAFE_LABELS.contains(label.toString().trim().toLowerCase(Locale.ROOT));
    }
}
