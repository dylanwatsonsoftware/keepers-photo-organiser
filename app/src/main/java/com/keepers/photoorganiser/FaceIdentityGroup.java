package com.keepers.photoorganiser;

import java.util.List;

public record FaceIdentityGroup(String id, List<FaceObservation> members) {
    public List<String> photoIds() {
        return members.stream().map(FaceObservation::photoId).toList();
    }
}
