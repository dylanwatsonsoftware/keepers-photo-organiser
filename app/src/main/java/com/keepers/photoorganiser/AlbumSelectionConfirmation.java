package com.keepers.photoorganiser;

final class AlbumSelectionConfirmation {
    private AlbumSelectionConfirmation() {}

    static boolean returnedToPhoto(boolean addToVisible, boolean albumSearchVisible,
            boolean editableSearchVisible) {
        return addToVisible && !albumSearchVisible && !editableSearchVisible;
    }
}
