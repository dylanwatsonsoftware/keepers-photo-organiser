package com.keepers.photoorganiser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FavouriteControlMatcherTest {
    @Test public void acceptsExactAustralianAndAmericanLabels() {
        assertTrue(FavouriteControlMatcher.matches("Favourite"));
        assertTrue(FavouriteControlMatcher.matches("Favorite"));
        assertTrue(FavouriteControlMatcher.matches("Add to favourites"));
        assertTrue(FavouriteControlMatcher.matches("Add to favorites"));
    }

    @Test public void rejectsControlsThatWouldReverseOrMisrouteTheAction() {
        assertFalse(FavouriteControlMatcher.matches("Unfavourite"));
        assertFalse(FavouriteControlMatcher.matches("Remove from favorites"));
        assertFalse(FavouriteControlMatcher.matches("Favorite people"));
        assertFalse(FavouriteControlMatcher.matches(null));
    }

    @Test public void recognisesAlreadyFavouriteStateWithoutMakingItClickable() {
        assertTrue(FavouriteControlMatcher.isAlreadyFavourite("Unfavourite"));
        assertTrue(FavouriteControlMatcher.isAlreadyFavourite("Unfavorite"));
        assertTrue(FavouriteControlMatcher.isAlreadyFavourite("Remove from favourites"));
        assertTrue(FavouriteControlMatcher.isAlreadyFavourite("Remove from favorites"));
        assertFalse(FavouriteControlMatcher.isAlreadyFavourite("Favourite"));
        assertFalse(FavouriteControlMatcher.isAlreadyFavourite(null));
    }

    @Test public void overflowControlRequiresTheExactGooglePhotosLabel() {
        assertTrue(FavouriteControlMatcher.isMoreOptions("More options"));
        assertTrue(FavouriteControlMatcher.isMoreOptions("  more options  "));
        assertFalse(FavouriteControlMatcher.isMoreOptions("Options"));
        assertFalse(FavouriteControlMatcher.isMoreOptions("More photos"));
        assertFalse(FavouriteControlMatcher.isMoreOptions(null));
    }
}
