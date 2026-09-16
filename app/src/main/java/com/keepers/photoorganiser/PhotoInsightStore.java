package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PhotoInsightStore {
    // Increment when feature extraction or recommendation rules require fresh analysis.
    private static final String ANALYSIS_SCHEMA = "v1";
    private final SharedPreferences preferences;
    private final KeeperSelectionStore keeperSelections;
    private final PhotoContextStore photoContexts;

    public PhotoInsightStore(Context context) {
        preferences = context.getSharedPreferences("photo_insights", Context.MODE_PRIVATE);
        keeperSelections = new KeeperSelectionStore(context);
        photoContexts = new PhotoContextStore(context);
    }

    public void save(List<PhotoFeatures> features, Map<String, PhotoStackPosition> stacks,
            Set<String> recommendations) {
        save(features, stacks, recommendations, Set.of());
    }

    public void save(List<PhotoFeatures> features, Map<String, PhotoStackPosition> stacks,
            Set<String> recommendations, Set<String> alternatives) {
        SharedPreferences.Editor editor = preferences.edit();
        for (PhotoFeatures feature : features) {
            PhotoStackPosition stack = stacks.get(feature.id());
            int position = stack == null ? 0 : stack.position();
            int size = stack == null ? 0 : stack.size();
            boolean recommended = recommendations.contains(feature.id());
            boolean alternative = alternatives.contains(feature.id());
            editor.putString(feature.id(), ANALYSIS_SCHEMA + "|" + feature.quality()
                    + "|" + position + "|" + size
                    + "|" + recommended + "|" + feature.focus() + "|" + feature.exposure()
                    + "|" + feature.composition() + "|" + feature.motionStability()
                    + "|" + alternative + "|" + feature.faceCount() + "|" + feature.smile()
                    + "|" + feature.eyesOpen() + "|" + feature.cameraFacing()
                    + "|" + feature.takenAtMillis() + "|" + feature.perceptualHash());
        }
        editor.apply();
    }

    public PhotoInsight load(String id) {
        String encoded = preferences.getString(id, null);
        if (encoded == null) return null;
        String[] parts = encoded.split("\\|");
        boolean current = parts.length == 16 && ANALYSIS_SCHEMA.equals(parts[0]);
        int offset = current ? 1 : 0;
        if (!current && parts.length != 4 && parts.length != 8 && parts.length != 9
                && parts.length != 12 && parts.length != 13) return null;
        try {
            double quality = Double.parseDouble(parts[offset]);
            int position = Integer.parseInt(parts[offset + 1]);
            int size = Integer.parseInt(parts[offset + 2]);
            boolean recommended = Boolean.parseBoolean(parts[offset + 3]);
            boolean alternative = parts.length >= offset + 9
                    && Boolean.parseBoolean(parts[offset + 8]);
            PhotoFeatures features = parts.length >= offset + 8
                    ? parseFeatures(id, parts, offset, current)
                    : new PhotoFeatures(id, 0, 0, quality);
            PhotoStackPosition stack = size > 1 ? new PhotoStackPosition(position, size) : null;
            String reason = alternative ? "A near-identical photo ranked slightly higher"
                    : recommended
                    ? stack == null ? "One of the strongest distinct recent shots"
                    : "Best detail score in this stack"
                    : stack == null ? "Below the current recommendation cutoff"
                    : "Another photo in this stack scored higher";
            return new PhotoInsight(quality, stack, recommended, alternative, reason,
                    PhotoAssessment.from(features, stack, keeperSelections.load().contains(id),
                            contextOrGeneral(id)));
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    public PhotoFeatures loadFeatures(String id) {
        String encoded = preferences.getString(id, null);
        if (encoded == null) return null;
        String[] parts = encoded.split("\\|");
        boolean current = parts.length == 16 && ANALYSIS_SCHEMA.equals(parts[0]);
        int offset = current ? 1 : 0;
        if (!current && parts.length != 8 && parts.length != 9 && parts.length != 12
                && parts.length != 13) return null;
        try {
            return parseFeatures(id, parts, offset, current);
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    public PhotoFeatures loadReusableFeatures(String id, long takenAtMillis) {
        String encoded = preferences.getString(id, null);
        if (encoded == null) return null;
        String[] parts = encoded.split("\\|");
        if (parts.length != 16 || !ANALYSIS_SCHEMA.equals(parts[0])) return null;
        try {
            PhotoFeatures features = parseFeatures(id, parts, 1, true);
            return features.takenAtMillis() == takenAtMillis ? features : null;
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    private static PhotoFeatures parseFeatures(String id, String[] parts, int offset,
            boolean current) {
        boolean extended = parts.length >= offset + 12;
        return new PhotoFeatures(id,
                current ? Long.parseLong(parts[offset + 13]) : 0,
                current ? Long.parseLong(parts[offset + 14]) : 0,
                Double.parseDouble(parts[offset]), Double.parseDouble(parts[offset + 4]),
                Double.parseDouble(parts[offset + 5]), Double.parseDouble(parts[offset + 6]),
                Double.parseDouble(parts[offset + 7]),
                extended ? Integer.parseInt(parts[offset + 9]) : 0,
                extended ? Double.parseDouble(parts[offset + 10]) : -1,
                extended ? Double.parseDouble(parts[offset + 11]) : -1,
                parts.length >= offset + 13 ? Double.parseDouble(parts[offset + 12]) : -1);
    }

    private PhotoContext contextOrGeneral(String id) {
        PhotoContext context = photoContexts.load(id);
        return context == null ? PhotoContext.general() : context;
    }
}
