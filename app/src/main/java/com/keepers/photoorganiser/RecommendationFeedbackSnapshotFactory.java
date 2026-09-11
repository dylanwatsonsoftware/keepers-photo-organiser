package com.keepers.photoorganiser;

import android.content.Context;
import android.content.pm.PackageManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RecommendationFeedbackSnapshotFactory {
    private RecommendationFeedbackSnapshotFactory() {}

    public static RecommendationFeedbackSnapshot create(Context context) {
        List<RecommendationFeedback> feedback = new RecommendationFeedbackStore(context).load();
        Map<String, List<String>> stacks = new PhotoStackStore(context).loadAll();
        PhotoInsightStore insights = new PhotoInsightStore(context);
        Set<String> hiddenIds = new HiddenPhotoStore(context).load();
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
                features, stacks, new KeeperSelectionStore(context).load());
        RecommendationExportMetadata metadata =
                new RecommendationExportIdentityStore(context).nextSnapshot();
        String appVersion = appVersion(context);
        String json = RecommendationFeedbackExport.toJson(
                feedback, comparisons, hiddenFeatures, appVersion, metadata);
        int evidenceCount = feedback.size() + comparisons.size() + hiddenFeatures.size();
        return new RecommendationFeedbackSnapshot(metadata.sourceId(),
                metadata.snapshotSequence(), appVersion, evidenceCount, json);
    }

    private static String appVersion(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0)
                    .versionName;
        } catch (PackageManager.NameNotFoundException impossible) {
            return "unknown";
        }
    }
}
