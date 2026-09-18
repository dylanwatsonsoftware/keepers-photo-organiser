package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GalleryMetadataOverlayTest {
    @Test public void displaysTheThreeHighestSignalsInDescendingOrder() {
        PhotoFeatures features = new PhotoFeatures("photo", 0, 0, .81,
                .92, .63, .88, .47, 0, -1, -1);

        assertEquals("Focus 92%\nComposition 88%\nDetail 81%",
                GalleryMetadataOverlay.topSignals(features, 3));
    }

    @Test public void roundsPercentagesAndUsesStableLabelsForTies() {
        PhotoFeatures features = new PhotoFeatures("photo", 0, 0, .5,
                .5, .5, .5, .5, 0, -1, -1);

        assertEquals("Focus 50%\nExposure 50%\nComposition 50%\nMotion 50%\nDetail 50%",
                GalleryMetadataOverlay.topSignals(features, 5));
    }

    @Test public void includesPortraitReadinessAmongTheHighestSignals() {
        PhotoFeatures features = new PhotoFeatures("portrait", 0, 0, .5,
                .5, .5, .5, .5, 1, .2, .95, .9);

        assertEquals("Eyes open 95%\nFacing camera 90%",
                GalleryMetadataOverlay.topSignals(features, 2));
    }

    @Test public void portraitOverlayShowsRankAndItsMostInfluentialWeakSignal() {
        PhotoFeatures features = new PhotoFeatures("portrait", 0, 0, .33,
                .57, .97, .92, .53, 1, .99, .20, .95);
        PhotoContext context = PhotoContext.of(PhotoContextType.PORTRAIT, .88);

        assertEquals("Type · Portrait 88%\nRank 56% · Limiting Eyes open 20%\n"
                        + "Exposure 97%\nFacing camera 95%",
                GalleryMetadataOverlay.topSignals(features, context, 2));
    }
}
