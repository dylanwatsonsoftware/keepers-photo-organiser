package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RecommendationFeedbackStore {
    private static final String PREFS = "recommendation_feedback";
    private final SharedPreferences preferences;

    public RecommendationFeedbackStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void save(RecommendationFeedback feedback) {
        PhotoFeatures f = feedback.features();
        String comment = Base64.encodeToString(feedback.comment().getBytes(StandardCharsets.UTF_8),
                Base64.NO_WRAP | Base64.URL_SAFE);
        String value = feedback.rating() + "|" + f.takenAtMillis() + "|" + f.perceptualHash()
                + "|" + f.quality() + "|" + f.focus() + "|" + f.exposure() + "|"
                + f.composition() + "|" + f.motionStability() + "|" + f.faceCount() + "|"
                + f.smile() + "|" + f.eyesOpen() + "|" + comment;
        preferences.edit().putString(f.id(), value).apply();
    }

    public RecommendationFeedback load(String photoId) {
        return decode(photoId, preferences.getString(photoId, null));
    }

    public List<RecommendationFeedback> load() {
        ArrayList<RecommendationFeedback> result = new ArrayList<>();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            RecommendationFeedback feedback = decode(entry.getKey(), String.valueOf(entry.getValue()));
            if (feedback != null) result.add(feedback);
        }
        return List.copyOf(result);
    }

    public void clear() { preferences.edit().clear().apply(); }

    private static RecommendationFeedback decode(String id, String encoded) {
        if (encoded == null) return null;
        String[] p = encoded.split("\\|", -1);
        if (p.length != 12) return null;
        try {
            PhotoFeatures features = new PhotoFeatures(id, Long.parseLong(p[1]), Long.parseLong(p[2]),
                    Double.parseDouble(p[3]), Double.parseDouble(p[4]), Double.parseDouble(p[5]),
                    Double.parseDouble(p[6]), Double.parseDouble(p[7]), Integer.parseInt(p[8]),
                    Double.parseDouble(p[9]), Double.parseDouble(p[10]));
            String comment = new String(Base64.decode(p[11], Base64.NO_WRAP | Base64.URL_SAFE),
                    StandardCharsets.UTF_8);
            return new RecommendationFeedback(features, Integer.parseInt(p[0]), comment);
        } catch (RuntimeException invalid) {
            return null;
        }
    }
}
