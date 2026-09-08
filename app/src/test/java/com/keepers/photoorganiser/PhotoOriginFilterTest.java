package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.Test;

public class PhotoOriginFilterTest {
    @Test public void localAndCloudFiltersOnlyShowTheirOwnOrigin() {
        List<String> photos = List.of("local", "cloud", "local-two");
        Map<String, PhotoOrigin> origins = Map.of(
                "local", PhotoOrigin.LOCAL, "cloud", PhotoOrigin.CLOUD,
                "local-two", PhotoOrigin.LOCAL);

        assertEquals(List.of("local", "local-two"),
                PhotoOriginFilter.apply(photos, origins, PhotoOrigin.LOCAL));
        assertEquals(List.of("cloud"),
                PhotoOriginFilter.apply(photos, origins, PhotoOrigin.CLOUD));
        assertEquals(photos, PhotoOriginFilter.apply(photos, origins, null));
    }
}
