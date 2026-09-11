package com.keepers.photoorganiser;

import android.content.Context;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public final class FeedbackSyncScheduler {
    public static final long REPEAT_INTERVAL = 12;
    public static final TimeUnit REPEAT_INTERVAL_UNIT = TimeUnit.HOURS;
    public static final String PERIODIC_WORK_NAME = "recommendation-feedback-periodic";
    private static final String IMMEDIATE_WORK_NAME = "recommendation-feedback-immediate";

    private FeedbackSyncScheduler() {}

    public static void configure(Context context) {
        WorkManager workManager = WorkManager.getInstance(context);
        if (!new FeedbackSyncPreferences(context).isEnabled()) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME);
            workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME);
            return;
        }
        Constraints connected = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest periodic = new PeriodicWorkRequest.Builder(
                FeedbackSyncWorker.class, REPEAT_INTERVAL, REPEAT_INTERVAL_UNIT)
                .setConstraints(connected).build();
        OneTimeWorkRequest immediate = new OneTimeWorkRequest.Builder(FeedbackSyncWorker.class)
                .setConstraints(connected).build();
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, periodic);
        workManager.enqueueUniqueWork(IMMEDIATE_WORK_NAME, ExistingWorkPolicy.KEEP, immediate);
    }
}
