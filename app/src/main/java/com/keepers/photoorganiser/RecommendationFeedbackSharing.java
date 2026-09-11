package com.keepers.photoorganiser;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

public final class RecommendationFeedbackSharing {
    private RecommendationFeedbackSharing() {}

    public static void share(Activity activity) {
        RecommendationFeedbackSnapshot snapshot =
                RecommendationFeedbackSnapshotFactory.create(activity);
        if (snapshot.evidenceCount() == 0) {
            Toast.makeText(activity, "No recommendation feedback to export yet",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Intent share = new Intent(Intent.ACTION_SEND).setType("application/json")
                .putExtra(Intent.EXTRA_SUBJECT, "Keepers recommendation feedback")
                .putExtra(Intent.EXTRA_TEXT, snapshot.json());
        activity.startActivity(Intent.createChooser(share, "Share private feedback export"));
    }
}
