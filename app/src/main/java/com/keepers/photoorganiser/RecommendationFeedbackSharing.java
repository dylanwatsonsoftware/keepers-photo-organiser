package com.keepers.photoorganiser;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RecommendationFeedbackSharing {
    private RecommendationFeedbackSharing() {}

    public static void share(Activity activity) {
        List<RecommendationFeedback> feedback =
                new RecommendationFeedbackStore(activity).load();
        Map<String, List<String>> stacks = new PhotoStackStore(activity).loadAll();
        PhotoInsightStore insights = new PhotoInsightStore(activity);
        Set<String> hiddenIds = new HiddenPhotoStore(activity).load();
        ArrayList<PhotoFeatures> features = new ArrayList<>();
        for (String photoId : stacks.keySet()) {
            PhotoFeatures feature = insights.loadFeatures(photoId);
            if (feature != null && !hiddenIds.contains(photoId)) features.add(feature);
        }
        ArrayList<PhotoFeatures> hiddenFeatures = new ArrayList<>();
        for (String photoId : hiddenIds) {
            PhotoFeatures feature = insights.loadFeatures(photoId);
            if (feature != null) hiddenFeatures.add(feature);
        }
        List<StackPreferenceComparison> comparisons = StackPreferenceComparison.from(
                features, stacks, new KeeperSelectionStore(activity).load());
        if (feedback.isEmpty() && comparisons.isEmpty() && hiddenFeatures.isEmpty()) {
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
        RecommendationExportMetadata exportMetadata =
                new RecommendationExportIdentityStore(activity).nextSnapshot();
        String json = RecommendationFeedbackExport.toJson(
                feedback, comparisons, hiddenFeatures, appVersion, exportMetadata);
        Intent share = new Intent(Intent.ACTION_SEND).setType("application/json")
                .putExtra(Intent.EXTRA_SUBJECT, "Keepers recommendation feedback")
                .putExtra(Intent.EXTRA_TEXT, json);
        activity.startActivity(Intent.createChooser(share, "Share private feedback export"));
    }
}
