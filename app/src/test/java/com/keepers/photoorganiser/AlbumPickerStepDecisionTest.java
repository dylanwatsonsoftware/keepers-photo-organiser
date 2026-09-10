package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AlbumPickerStepDecisionTest {
    @Test public void visibleTargetAlbumAlwaysWinsOverPickerAndSearchControls() {
        assertEquals(AlbumPickerStepDecision.SELECT_VISIBLE_ALBUM,
                AlbumPickerStepDecision.decide(true, true, true, 5_000, 0));
    }

    @Test public void albumPickerIsOpenedWhenTheTargetIsNotYetVisible() {
        assertEquals(AlbumPickerStepDecision.OPEN_ALBUM_PICKER,
                AlbumPickerStepDecision.decide(false, true, true, 5_000, 0));
    }

    @Test public void droppedAddToClickIsRetriedOnlyAfterTheUiTimeout() {
        assertEquals(AlbumPickerStepDecision.WAIT,
                AlbumPickerStepDecision.decide(false, false, true, 1_199, 0));
        assertEquals(AlbumPickerStepDecision.RETRY_ADD_TO,
                AlbumPickerStepDecision.decide(false, false, true, 1_200, 0));
    }

    @Test public void addToRetriesAreBounded() {
        assertEquals(AlbumPickerStepDecision.WAIT,
                AlbumPickerStepDecision.decide(false, false, true, 5_000,
                        AlbumPickerStepDecision.MAX_ADD_TO_RETRIES));
    }
}
