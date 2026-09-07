package com.keepers.photoorganiser;

import java.util.List;

public final class PhotoActionPlanner {
    private PhotoActionPlanner() {}

    public static PhotoActionPlan forPhotos(List<String> photoUris) {
        if (photoUris.isEmpty()) {
            throw new IllegalArgumentException("Select at least one photo");
        }

        if (photoUris.size() == 1) {
            return new PhotoActionPlan(PhotoActionPlan.Kind.OPEN_EXISTING, false);
        }

        return new PhotoActionPlan(PhotoActionPlan.Kind.SHARE_EXPERIMENT, true);
    }
}
