package com.keepers.photoorganiser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AlbumSelectionConfirmationTest {
    @Test public void photoAddControlAfterPickerClosesConfirmsTheSelection() {
        assertTrue(AlbumSelectionConfirmation.returnedToPhoto(true, false, false));
    }

    @Test public void albumSearchOrEditableSearchMeansThePickerIsStillOpen() {
        assertFalse(AlbumSelectionConfirmation.returnedToPhoto(true, true, false));
        assertFalse(AlbumSelectionConfirmation.returnedToPhoto(true, false, true));
    }

    @Test public void ambiguousScreenNeverRecordsACompletedAlbumChange() {
        assertFalse(AlbumSelectionConfirmation.returnedToPhoto(false, false, false));
    }
}
