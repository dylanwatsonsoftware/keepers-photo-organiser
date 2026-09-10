package com.keepers.photoorganiser;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Toast;
import java.util.List;

public final class RecommendationFeedbackSharing {
    private RecommendationFeedbackSharing() {}

    public static void share(Activity activity) {
        List<RecommendationFeedback> feedback =
                new RecommendationFeedbackStore(activity).load();
        if (feedback.isEmpty()) {
            Toast.makeText(activity, "No recommendation feedback to export yet",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String appVersion;
        try {
            appVersion = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException impossible) {
            appVersion = "unknown";
        }
        String json = RecommendationFeedbackExport.toJson(feedback, appVersion);
        Intent share = new Intent(Intent.ACTION_SEND).setType("application/json")
                .putExtra(Intent.EXTRA_SUBJECT, "Keepers recommendation feedback")
                .putExtra(Intent.EXTRA_TEXT, json);
        activity.startActivity(Intent.createChooser(share, "Share private feedback export"));
    }
}
