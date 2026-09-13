package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class VideoInsightStoreTest {
    @Test public void persistsVersionedDerivedVideoSignals() {
        Context context = RuntimeEnvironment.getApplication();
        VideoInsightStore store = new VideoInsightStore(context);
        store.clear();
        VideoFeatures features = new VideoFeatures(VideoFeatures.SCHEMA_VERSION, "video", 20_000,
                3, .7, .8, .9, .6, .75, .1, .2);

        store.save(features);

        assertEquals(features, store.load("video"));
        store.clear();
    }
}
