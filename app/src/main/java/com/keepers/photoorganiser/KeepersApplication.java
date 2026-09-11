package com.keepers.photoorganiser;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.work.Configuration;

public final class KeepersApplication extends Application implements Configuration.Provider {
    @NonNull @Override public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder().build();
    }

    @Override public void onCreate() {
        super.onCreate();
        FeedbackSyncScheduler.configure(this);
    }
}
