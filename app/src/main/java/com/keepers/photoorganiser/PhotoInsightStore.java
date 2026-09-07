package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PhotoInsightStore {
    private final SharedPreferences preferences;

    public PhotoInsightStore(Context context) {
        preferences = context.getSharedPreferences("photo_insights", Context.MODE_PRIVATE);
    }

    public void save(List<PhotoFeatures> features, Map<String, PhotoStackPosition> stacks,
            Set<String> recommendations) {
        SharedPreferences.Editor editor = preferences.edit().clear();
        for (PhotoFeatures feature : features) {
            PhotoStackPosition stack = stacks.get(feature.id());
            int position = stack == null ? 0 : stack.position();
            int size = stack == null ? 0 : stack.size();
            boolean recommended = recommendations.contains(feature.id());
            editor.putString(feature.id(), feature.quality() + "|" + position + "|" + size
                    + "|" + recommended);
        }
        editor.apply();
    }

    public PhotoInsight load(String id) {
        String encoded = preferences.getString(id, null);
        if (encoded == null) return null;
        String[] parts = encoded.split("\\|");
        if (parts.length != 4) return null;
        try {
            double quality = Double.parseDouble(parts[0]);
            int position = Integer.parseInt(parts[1]);
            int size = Integer.parseInt(parts[2]);
            boolean recommended = Boolean.parseBoolean(parts[3]);
            PhotoStackPosition stack = size > 1 ? new PhotoStackPosition(position, size) : null;
            String reason = recommended
                    ? stack == null ? "One of the strongest distinct recent shots"
                    : "Best detail score in this stack"
                    : stack == null ? "Below the current recommendation cutoff"
                    : "Another photo in this stack scored higher";
            return new PhotoInsight(quality, stack, recommended, reason);
        } catch (NumberFormatException invalid) {
            return null;
        }
    }
}
