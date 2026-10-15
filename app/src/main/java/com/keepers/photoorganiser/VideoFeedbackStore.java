package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class VideoFeedbackStore {
    private final SharedPreferences preferences;
    private final VideoInsightStore insights;

    public VideoFeedbackStore(Context context) {
        preferences = context.getSharedPreferences("video_feedback_v1", Context.MODE_PRIVATE);
        insights = new VideoInsightStore(context);
    }

    public void save(VideoFeedback feedback) {
        VideoFeatures f = feedback.features();
        String comment = Base64.encodeToString(feedback.comment().getBytes(StandardCharsets.UTF_8),
                Base64.NO_WRAP | Base64.URL_SAFE);
        preferences.edit().putString(f.id(), feedback.rating() + "|" + f.schemaVersion() + "|"
                + f.durationMillis() + "|" + f.sampledFrames() + "|" + f.detail() + "|" + f.focus()
                + "|" + f.exposure() + "|" + f.composition() + "|" + f.frameStability() + "|"
                + f.blackFrameRate() + "|" + f.frozenFrameRate() + "|" + comment).apply();
    }

    public void record(String id, int rating) {
        VideoFeatures features = insights.load(id);
        if (features != null) record(features, rating);
    }

    public void record(VideoFeatures features, int rating) {
        VideoFeedback previous = load(features.id());
        save(new VideoFeedback(features, rating, previous == null ? "" : previous.comment()));
    }

    public VideoFeedback load(String id) {
        String encoded = preferences.getString(id, null);
        if (encoded == null) return null;
        String[] p = encoded.split("\\|", -1);
        if (p.length != 12) return null;
        try {
            VideoFeatures f = new VideoFeatures(Integer.parseInt(p[1]), id, Long.parseLong(p[2]),
                    Integer.parseInt(p[3]), Double.parseDouble(p[4]), Double.parseDouble(p[5]),
                    Double.parseDouble(p[6]), Double.parseDouble(p[7]), Double.parseDouble(p[8]),
                    Double.parseDouble(p[9]), Double.parseDouble(p[10]));
            return new VideoFeedback(f, Integer.parseInt(p[0]), new String(Base64.decode(p[11],
                    Base64.NO_WRAP | Base64.URL_SAFE), StandardCharsets.UTF_8));
        } catch (RuntimeException invalid) { return null; }
    }

    public List<VideoFeedback> load() {
        ArrayList<VideoFeedback> result = new ArrayList<>();
        for (String id : preferences.getAll().keySet()) {
            VideoFeedback item = load(id);
            if (item != null) result.add(item);
        }
        return List.copyOf(result);
    }

    public void clear() { preferences.edit().clear().apply(); }
}
