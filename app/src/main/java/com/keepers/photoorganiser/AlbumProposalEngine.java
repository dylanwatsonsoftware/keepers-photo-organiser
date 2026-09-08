package com.keepers.photoorganiser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AlbumProposalEngine {
    private AlbumProposalEngine() {}

    public static List<AlbumAssignment> propose(Set<String> keeperPhotoIds,
            List<FaceIdentityGroup> groups, Map<String, String> groupAssignments,
            List<TrackedPerson> people) {
        HashMap<String, TrackedPerson> eligiblePeople = new HashMap<>();
        for (TrackedPerson person : people) if (person.tracked() && !person.albumName().isBlank())
            eligiblePeople.put(person.id(), person);

        ArrayList<AlbumAssignment> result = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        for (FaceIdentityGroup group : groups) {
            TrackedPerson person = eligiblePeople.get(groupAssignments.get(group.id()));
            if (person == null) continue;
            for (String photoId : group.photoIds()) {
                String key = photoId + "\n" + person.id();
                if (keeperPhotoIds.contains(photoId) && seen.add(key))
                    result.add(new AlbumAssignment(photoId, person.id(), person.name(),
                            person.albumName(), true));
            }
        }
        result.sort(Comparator.comparing(AlbumAssignment::photoId)
                .thenComparing(AlbumAssignment::personName));
        return List.copyOf(result);
    }
}
