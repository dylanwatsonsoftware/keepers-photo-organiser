package com.keepers.photoorganiser;

import java.util.List;
import java.util.Map;

public final class PhotoOriginFilter {
    private PhotoOriginFilter() {}

    public static List<String> apply(List<String> photos, Map<String, PhotoOrigin> origins,
            PhotoOrigin requested) {
        if (requested == null) return List.copyOf(photos);
        return photos.stream().filter(photo -> origins.getOrDefault(photo, PhotoOrigin.LOCAL)
                == requested).toList();
    }
}
