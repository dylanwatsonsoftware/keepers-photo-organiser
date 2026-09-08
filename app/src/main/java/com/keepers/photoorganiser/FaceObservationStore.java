package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FaceObservationStore {
    private static final String IDS = "photo_ids";
    private final SharedPreferences preferences;

    public FaceObservationStore(Context context) {
        preferences = context.getSharedPreferences("face_observations", Context.MODE_PRIVATE);
    }

    public void save(String photoId, List<FaceObservation> faces) {
        Set<String> ids = new HashSet<>(preferences.getStringSet(IDS, Set.of()));
        ids.add(photoId);
        ArrayList<String> encoded = new ArrayList<>();
        for (FaceObservation face : faces) encoded.add(face.faceIndex() + "," + face.left() + ","
                + face.top() + "," + face.right() + "," + face.bottom() + "," + face.smile()
                + "," + face.leftEyeOpen() + "," + face.rightEyeOpen() + "," + face.yaw()
                + "," + face.roll());
        preferences.edit().putStringSet(IDS, ids)
                .putString(photoId, String.join(";", encoded)).apply();
    }

    public List<FaceObservation> load(String photoId) {
        String value = preferences.getString(photoId, "");
        if (value.isEmpty()) return List.of();
        ArrayList<FaceObservation> result = new ArrayList<>();
        try {
            for (String item : value.split(";")) {
                String[] p = item.split(",");
                result.add(new FaceObservation(photoId, Integer.parseInt(p[0]),
                        Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]),
                        Double.parseDouble(p[4]), Double.parseDouble(p[5]), Double.parseDouble(p[6]),
                        Double.parseDouble(p[7]), Double.parseDouble(p[8]), Double.parseDouble(p[9])));
            }
        } catch (RuntimeException invalid) { return List.of(); }
        return result;
    }

    public int observationCount() {
        int count = 0;
        for (String id : preferences.getStringSet(IDS, Set.of())) count += load(id).size();
        return count;
    }
}
