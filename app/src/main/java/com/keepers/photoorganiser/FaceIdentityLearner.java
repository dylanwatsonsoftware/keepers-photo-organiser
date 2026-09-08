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
            String personId = groupAssignments.get(group.id());
            if (personId == null || personId.isBlank()) continue;
            for (FaceObservation face : group.members())
                evidence.put(FaceCorrectionStore.key(face), personId);
        }
        evidence.putAll(corrections);
        return predict(observations, evidence, maximumDistance);
    }

    public static Map<String, String> predict(List<FaceObservation> observations,
            Map<String, String> corrections, double maximumDistance) {
        ArrayList<Example> examples = new ArrayList<>();
        for (FaceObservation face : observations) {
            String person = corrections.get(FaceCorrectionStore.key(face));
            double[] descriptor = FaceDescriptor.decode(face.descriptor());
            if (person != null && !FaceCorrectionStore.IGNORE.equals(person)
                    && descriptor.length > 0) examples.add(new Example(person, descriptor));
        }
        HashMap<String, String> result = new HashMap<>();
        for (FaceObservation face : observations) {
            String key = FaceCorrectionStore.key(face);
            if (corrections.containsKey(key)) continue;
            double[] descriptor = FaceDescriptor.decode(face.descriptor());
            HashMap<String, Double> nearestByPerson = new HashMap<>();
            for (Example example : examples) {
                double distance = distance(descriptor, example.descriptor);
                nearestByPerson.merge(example.personId, distance, Math::min);
            }
            List<Map.Entry<String, Double>> nearest = nearestByPerson.entrySet().stream()
                    .sorted(Comparator.comparingDouble(Map.Entry::getValue)).toList();
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

    private record Example(String personId, double[] descriptor) {}
}
