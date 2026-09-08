package com.keepers.photoorganiser;

import java.util.ArrayList;
import java.util.List;

public final class FaceClusterer {
    private FaceClusterer() {}

    public static List<FaceIdentityGroup> cluster(List<FaceObservation> observations,
            double maximumDistance) {
        ArrayList<MutableGroup> groups = new ArrayList<>();
        for (FaceObservation observation : observations) {
            double[] descriptor = FaceDescriptor.decode(observation.descriptor());
            if (descriptor.length == 0) continue;
            MutableGroup best = null;
            double bestDistance = Double.MAX_VALUE;
            for (MutableGroup group : groups) {
                double distance = cosineDistance(descriptor, group.centroid);
                if (distance < bestDistance) { best = group; bestDistance = distance; }
            }
            if (best == null || bestDistance > maximumDistance) groups.add(new MutableGroup(observation,
                    descriptor));
            else best.add(observation, descriptor);
        }
        ArrayList<FaceIdentityGroup> result = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) result.add(new FaceIdentityGroup(
                "face-group-" + (i + 1), List.copyOf(groups.get(i).members)));
        return List.copyOf(result);
    }

    private static double cosineDistance(double[] left, double[] right) {
        if (left.length != right.length) return Double.MAX_VALUE;
        double dot = 0, leftMagnitude = 0, rightMagnitude = 0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftMagnitude += left[i] * left[i];
            rightMagnitude += right[i] * right[i];
        }
        if (leftMagnitude == 0 || rightMagnitude == 0) return Double.MAX_VALUE;
        return 1 - dot / Math.sqrt(leftMagnitude * rightMagnitude);
    }

    private static final class MutableGroup {
        final ArrayList<FaceObservation> members = new ArrayList<>();
        double[] centroid;
        MutableGroup(FaceObservation first, double[] descriptor) {
            members.add(first); centroid = descriptor.clone();
        }
        void add(FaceObservation observation, double[] descriptor) {
            int oldCount = members.size();
            members.add(observation);
            for (int i = 0; i < centroid.length; i++)
                centroid[i] = (centroid[i] * oldCount + descriptor[i]) / (oldCount + 1);
        }
    }
}
