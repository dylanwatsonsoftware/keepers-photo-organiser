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
        return propose(keeperPhotoIds, groups, groupAssignments, Map.of(), people);
    }

    public static List<AlbumAssignment> propose(Set<String> keeperPhotoIds,
            List<FaceIdentityGroup> groups, Map<String, String> groupAssignments,
            Map<String, String> faceCorrections, List<TrackedPerson> people) {
        HashMap<String, TrackedPerson> eligiblePeople = new HashMap<>();
        for (TrackedPerson person : people) if (person.tracked() && !person.albumName().isBlank())
            eligiblePeople.put(person.id(), person);

        ArrayList<AlbumAssignment> result = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        List<FaceObservation> allFaces = groups.stream().flatMap(group -> group.members().stream())
                .toList();
        Map<String, String> learned = FaceIdentityLearner.predict(allFaces, groups,
                groupAssignments, faceCorrections, .15);
        for (FaceIdentityGroup group : groups) {
            String groupPerson = groupAssignments.get(group.id());
            for (FaceObservation face : group.members()) {
                String faceKey = FaceCorrectionStore.key(face);
                String personId = faceCorrections.containsKey(faceKey)
                        ? faceCorrections.get(faceKey)
                        : groupPerson != null ? groupPerson : learned.get(faceKey);
                if (FaceCorrectionStore.IGNORE.equals(personId)) continue;
                TrackedPerson person = eligiblePeople.get(personId);
                if (person == null) continue;
                String photoId = face.photoId();
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
