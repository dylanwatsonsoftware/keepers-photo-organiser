package com.keepers.photoorganiser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FaceIdentityLearner {
    private static final double AMBIGUITY_MARGIN = .04;
    private FaceIdentityLearner() {}

    public static Map<String, String> predict(List<FaceObservation> observations,
            List<FaceIdentityGroup> groups, Map<String, String> groupAssignments,
            Map<String, String> corrections, double maximumDistance) {
        HashMap<String, String> evidence = new HashMap<>();
        for (FaceIdentityGroup group : groups) {
            String personId = FaceGroupAssignmentResolver.personFor(group, groupAssignments);
            if (personId.isBlank()) continue;
            for (FaceObservation face : group.members())
                evidence.put(FaceCorrectionStore.key(face), personId);
        }
        evidence.putAll(corrections);
        return predict(observations, evidence, maximumDistance);
    }

    public static Map<String, String> predict(List<FaceObservation> observations,
            Map<String, String> corrections, double maximumDistance) {
        HashMap<String, Profile> profiles = new HashMap<>();
        for (FaceObservation face : observations) {
            String person = corrections.get(FaceCorrectionStore.key(face));
            double[] descriptor = FaceDescriptor.decode(face.descriptor());
            if (person != null && !FaceCorrectionStore.IGNORE.equals(person)
                    && descriptor.length > 0)
                profiles.computeIfAbsent(person, ignored -> new Profile(descriptor.length))
                        .add(descriptor);
        }
        HashMap<String, String> result = new HashMap<>();
        for (FaceObservation face : observations) {
            String key = FaceCorrectionStore.key(face);
            if (corrections.containsKey(key)) continue;
            double[] descriptor = FaceDescriptor.decode(face.descriptor());
            HashMap<String, Double> nearestByPerson = new HashMap<>();
            for (Map.Entry<String, Profile> profile : profiles.entrySet())
                nearestByPerson.put(profile.getKey(),
                        distance(descriptor, profile.getValue().centroid()));
            List<Map.Entry<String, Double>> nearest = nearestByPerson.entrySet().stream()
                    .sorted(Comparator.<Map.Entry<String, Double>>comparingDouble(
                            Map.Entry::getValue).thenComparing(Map.Entry::getKey)).toList();
            if (nearest.isEmpty() || nearest.get(0).getValue() > maximumDistance) continue;
            if (nearest.size() > 1 && nearest.get(1).getValue() - nearest.get(0).getValue()
                    < AMBIGUITY_MARGIN) continue;
            result.put(key, nearest.get(0).getKey());
        }
        return Map.copyOf(result);
    }

    private static double distance(double[] left, double[] right) {
        if (left.length == 0 || left.length != right.length) return Double.MAX_VALUE;
        double dot = 0, leftSize = 0, rightSize = 0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftSize += left[i] * left[i];
            rightSize += right[i] * right[i];
        }
        if (leftSize == 0 || rightSize == 0) return Double.MAX_VALUE;
        return 1 - dot / Math.sqrt(leftSize * rightSize);
    }

    private static final class Profile {
        private final double[] total;
        private int count;

        Profile(int dimensions) { total = new double[dimensions]; }

        void add(double[] descriptor) {
            if (descriptor.length != total.length) return;
            for (int index = 0; index < total.length; index++) total[index] += descriptor[index];
            count++;
        }

        double[] centroid() {
            double[] centroid = total.clone();
            if (count > 0) for (int index = 0; index < centroid.length; index++)
                centroid[index] /= count;
            return centroid;
        }
    }
}
