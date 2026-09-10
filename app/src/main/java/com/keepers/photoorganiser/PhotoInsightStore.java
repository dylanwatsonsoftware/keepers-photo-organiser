package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PhotoInsightStore {
    private final SharedPreferences preferences;
    private final KeeperSelectionStore keeperSelections;

    public PhotoInsightStore(Context context) {
        preferences = context.getSharedPreferences("photo_insights", Context.MODE_PRIVATE);
        keeperSelections = new KeeperSelectionStore(context);
    }

    public void save(List<PhotoFeatures> features, Map<String, PhotoStackPosition> stacks,
            Set<String> recommendations) {
        save(features, stacks, recommendations, Set.of());
    }

    public void save(List<PhotoFeatures> features, Map<String, PhotoStackPosition> stacks,
            Set<String> recommendations, Set<String> alternatives) {
        SharedPreferences.Editor editor = preferences.edit().clear();
        for (PhotoFeatures feature : features) {
            PhotoStackPosition stack = stacks.get(feature.id());
            int position = stack == null ? 0 : stack.position();
            int size = stack == null ? 0 : stack.size();
            boolean recommended = recommendations.contains(feature.id());
            boolean alternative = alternatives.contains(feature.id());
            editor.putString(feature.id(), feature.quality() + "|" + position + "|" + size
                    + "|" + recommended + "|" + feature.focus() + "|" + feature.exposure()
                    + "|" + feature.composition() + "|" + feature.motionStability()
                    + "|" + alternative + "|" + feature.faceCount() + "|" + feature.smile()
                    + "|" + feature.eyesOpen());
        }
        editor.apply();
    }

    public PhotoInsight load(String id) {
        String encoded = preferences.getString(id, null);
        if (encoded == null) return null;
        String[] parts = encoded.split("\\|");
        if (parts.length != 4 && parts.length != 8 && parts.length != 9
                && parts.length != 12) return null;
        try {
            double quality = Double.parseDouble(parts[0]);
            int position = Integer.parseInt(parts[1]);
            int size = Integer.parseInt(parts[2]);
            boolean recommended = Boolean.parseBoolean(parts[3]);
            boolean alternative = parts.length >= 9 && Boolean.parseBoolean(parts[8]);
            PhotoFeatures features = parts.length >= 8
                    ? new PhotoFeatures(id, 0, 0, quality, Double.parseDouble(parts[4]),
                            Double.parseDouble(parts[5]), Double.parseDouble(parts[6]),
                            Double.parseDouble(parts[7]), parts.length == 12
                            ? Integer.parseInt(parts[9]) : 0, parts.length == 12
                            ? Double.parseDouble(parts[10]) : -1, parts.length == 12
                            ? Double.parseDouble(parts[11]) : -1)
                    : new PhotoFeatures(id, 0, 0, quality);
            PhotoStackPosition stack = size > 1 ? new PhotoStackPosition(position, size) : null;
            String reason = alternative ? "A near-identical photo ranked slightly higher"
                    : recommended
                    ? stack == null ? "One of the strongest distinct recent shots"
                    : "Best detail score in this stack"
                    : stack == null ? "Below the current recommendation cutoff"
                    : "Another photo in this stack scored higher";
            return new PhotoInsight(quality, stack, recommended, alternative, reason,
                    PhotoAssessment.from(features, stack, keeperSelections.load().contains(id)));
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    public PhotoFeatures loadFeatures(String id) {
        String encoded = preferences.getString(id, null);
        if (encoded == null) return null;
        String[] parts = encoded.split("\\|");
        if (parts.length != 8 && parts.length != 9 && parts.length != 12) return null;
        try {
            return new PhotoFeatures(id, 0, 0, Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[4]), Double.parseDouble(parts[5]),
                    Double.parseDouble(parts[6]), Double.parseDouble(parts[7]),
                    parts.length == 12 ? Integer.parseInt(parts[9]) : 0,
                    parts.length == 12 ? Double.parseDouble(parts[10]) : -1,
                    parts.length == 12 ? Double.parseDouble(parts[11]) : -1);
        } catch (NumberFormatException invalid) {
            return null;
        }
    }
}
