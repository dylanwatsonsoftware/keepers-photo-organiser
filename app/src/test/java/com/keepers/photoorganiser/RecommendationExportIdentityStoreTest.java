package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class RecommendationExportIdentityStoreTest {
    @Test public void repeatedExportsKeepTheirAnonymousSourceAndAdvanceTheSnapshot() {
        Context context = RuntimeEnvironment.getApplication();
        RecommendationExportIdentityStore store = new RecommendationExportIdentityStore(context);
        store.clear();

        RecommendationExportMetadata first = store.nextSnapshot();
        RecommendationExportMetadata second = store.nextSnapshot();

        assertEquals(first.sourceId(), second.sourceId());
        assertEquals(1, first.snapshotSequence());
        assertEquals(2, second.snapshotSequence());
    }
}
