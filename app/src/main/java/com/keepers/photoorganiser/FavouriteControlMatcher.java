package com.keepers.photoorganiser;

import java.util.Locale;
import java.util.Set;

public final class FavouriteControlMatcher {
    private static final Set<String> SAFE_LABELS = Set.of(
            "favourite", "favorite", "add to favourites", "add to favorites");
    private static final Set<String> ALREADY_FAVOURITE_LABELS = Set.of(
            "unfavourite", "unfavorite", "remove from favourites", "remove from favorites");
    private FavouriteControlMatcher() {}

    public static boolean matches(CharSequence label) {
        return label != null && SAFE_LABELS.contains(label.toString().trim().toLowerCase(Locale.ROOT));
    }

    public static boolean isAlreadyFavourite(CharSequence label) {
        return label != null
                && ALREADY_FAVOURITE_LABELS.contains(label.toString().trim().toLowerCase(Locale.ROOT));
    }
}
