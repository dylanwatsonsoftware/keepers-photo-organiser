package com.keepers.photoorganiser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NamedFaceResolver {
    private NamedFaceResolver() {}

    public static Map<String, Set<String>> resolve(List<FaceIdentityGroup> groups,
            Map<String, String> groupAssignments, Map<String, String> faceCorrections) {
        HashMap<String, HashSet<String>> peopleByPhoto = new HashMap<>();
        for (FaceIdentityGroup group : groups) {
            String groupPerson = FaceGroupAssignmentResolver.personFor(group, groupAssignments);
            for (FaceObservation face : group.members()) {
                String faceKey = FaceCorrectionStore.key(face);
                String person = faceCorrections.containsKey(faceKey)
                        ? faceCorrections.get(faceKey) : groupPerson;
                if (person == null || person.isBlank()) person = group.id();
                if (person == null || person.isBlank()
                        || FaceCorrectionStore.IGNORE.equals(person)) continue;
                peopleByPhoto.computeIfAbsent(face.photoId(), ignored -> new HashSet<>())
                        .add(person);
            }
        }
        HashMap<String, Set<String>> result = new HashMap<>();
        for (Map.Entry<String, HashSet<String>> entry : peopleByPhoto.entrySet())
            result.put(entry.getKey(), Set.copyOf(entry.getValue()));
        return Map.copyOf(result);
    }
}
