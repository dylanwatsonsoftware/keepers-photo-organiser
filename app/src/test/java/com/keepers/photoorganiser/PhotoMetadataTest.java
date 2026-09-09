package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import org.junit.Test;

public class PhotoMetadataTest {
    @Test public void formatsTakenDateInTheRequestedFriendlyStyle() {
        PhotoMetadata metadata = new PhotoMetadata(1788864420000L, "", "", 0, 0, "", 0);

        assertEquals("Tue, Sep 8, 2026 • 6:47 PM",
                metadata.formattedDate(ZoneId.of("Australia/Perth"), Locale.US));
    }

    @Test public void buildsACompactTechnicalSummaryFromAvailableValues() {
        PhotoMetadata metadata = new PhotoMetadata(0, "", "", 4032, 3024,
                "image/jpeg", 3_250_000);

        assertEquals("4032 × 3024  •  JPEG  •  3.3 MB", metadata.technicalSummary(Locale.US));
    }

    @Test public void omitsMissingTechnicalValues() {
        PhotoMetadata metadata = new PhotoMetadata(0, "", "", 0, 0, "image/png", 0);

        assertEquals("PNG", metadata.technicalSummary(Locale.US));
    }

    @Test public void rejectsTheExifZeroCoordinatePlaceholder() {
        assertEquals("", PhotoMetadata.locationFromCoordinates(0, 0));
    }

    @Test public void formatsValidExifCoordinates() {
        assertEquals("31.95230° S, 115.86130° E",
                PhotoMetadata.locationFromCoordinates(-31.9523f, 115.8613f));
    }
}
