package com.keepers.photoorganiser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AlbumControlMatcherTest {
    @Test public void addControlRequiresAnExactLabel() {
        assertTrue(AlbumControlMatcher.isAddToAlbum("Add to"));
        assertTrue(AlbumControlMatcher.isAddToAlbum("Add to album"));
        assertFalse(AlbumControlMatcher.isAddToAlbum("Add"));
        assertFalse(AlbumControlMatcher.isAddToAlbum("Add to shared album"));
    }

    @Test public void albumRequiresAnExactNonEmptyName() {
        assertTrue(AlbumControlMatcher.isAlbum("Ada", "ada"));
        assertTrue(AlbumControlMatcher.isAlbum("  Ada  ", "Ada"));
        assertFalse(AlbumControlMatcher.isAlbum("Ada 2026", "Ada"));
        assertFalse(AlbumControlMatcher.isAlbum("", ""));
        assertFalse(AlbumControlMatcher.isAlbum(null, "Ada"));
    }

    @Test public void albumPickerOptionRequiresExactAlbumLabel() {
        assertTrue(AlbumControlMatcher.isAlbumPickerOption("Album"));
        assertTrue(AlbumControlMatcher.isAlbumPickerOption("  album  "));
        assertFalse(AlbumControlMatcher.isAlbumPickerOption("Shared album"));
        assertFalse(AlbumControlMatcher.isAlbumPickerOption(null));
    }

    @Test public void albumSearchRequiresTheExactGooglePhotosControl() {
        assertTrue(AlbumControlMatcher.isAlbumSearch("Search all albums"));
        assertTrue(AlbumControlMatcher.isAlbumSearch("  search all albums  "));
        assertFalse(AlbumControlMatcher.isAlbumSearch("Search photos"));
        assertFalse(AlbumControlMatcher.isAlbumSearch("Search albums nearby"));
        assertFalse(AlbumControlMatcher.isAlbumSearch(null));
    }
}
