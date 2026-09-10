package com.keepers.photoorganiser;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class KeeperSelectionRecovery {
    private static final String PREFS = "keeper_selection_recovery";
    private static final String CLEAR_ACTION_REMOVED = "clear_action_removed_v1";

    private KeeperSelectionRecovery() {}

    static void runOnce(Context context) {
        SharedPreferences recovery = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (recovery.getBoolean(CLEAR_ACTION_REMOVED, false)) return;

        KeeperSelectionStore selections = new KeeperSelectionStore(context);
        if (selections.load().isEmpty()) {
            Set<String> recovered = new HashSet<>();
            Map<String, Integer> ratings = new HashMap<>();
            for (RecommendationFeedback item : new RecommendationFeedbackStore(context).load()) {
                ratings.put(item.features().id(), item.rating());
                if (item.rating() == RecommendationFeedback.LOVED) {
                    recovered.add(item.features().id());
                }
            }
            for (String photo : new ReviewedPhotoStore(context).load()) {
                if (ratings.get(photo) == null
                        || ratings.get(photo) != RecommendationFeedback.NOT_FOR_ME) {
                    recovered.add(photo);
                }
            }
            if (!recovered.isEmpty()) selections.replace(recovered);
        }

        recovery.edit().putBoolean(CLEAR_ACTION_REMOVED, true).apply();
    }
}
