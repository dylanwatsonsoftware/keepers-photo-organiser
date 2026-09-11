package com.keepers.photoorganiser;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.FirebaseApp;

public final class FeedbackSyncWorker extends Worker {
    public FeedbackSyncWorker(@NonNull Context context,
            @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    @NonNull @Override public Result doWork() {
        Context context = getApplicationContext();
        FeedbackSyncPreferences preferences = new FeedbackSyncPreferences(context);
        if (!preferences.isEnabled()) return Result.success();
        FirebaseFeedbackSnapshotUploader uploader = new FirebaseFeedbackSnapshotUploader();
        FirebaseApp app = uploader.configuredApp(context);
        RecommendationFeedbackSnapshot snapshot =
                RecommendationFeedbackSnapshotFactory.create(context);
        FeedbackSyncPolicy.Action action = FeedbackSyncPolicy.decide(
                true, app != null, snapshot);
        if (action == FeedbackSyncPolicy.Action.WAIT_FOR_CONFIGURATION) {
            preferences.recordFailure("Firebase setup is incomplete");
            return Result.success();
        }
        if (action == FeedbackSyncPolicy.Action.SKIP_EMPTY) return Result.success();
        if (action == FeedbackSyncPolicy.Action.PAYLOAD_TOO_LARGE) {
            preferences.recordFailure("Feedback snapshot is too large to upload");
            return Result.failure();
        }
        try {
            uploader.upload(app, snapshot);
            preferences.recordSuccess(snapshot.snapshotSequence(), System.currentTimeMillis());
            return Result.success();
        } catch (Exception failure) {
            String message = failure.getMessage();
            preferences.recordFailure(message == null || message.isBlank()
                    ? failure.getClass().getSimpleName() : message);
            return Result.retry();
        }
    }
}
