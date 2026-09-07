package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;
import android.content.Context;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SuggestionStoreTest {
    @Test public void replacesPersistedRecommendationsForFullscreenReview() {
        Context context = RuntimeEnvironment.getApplication();
        SuggestionStore store = new SuggestionStore(context);
        store.save(Set.of("one", "two"));
        store.save(Set.of("three"));
        assertEquals(Set.of("three"), new SuggestionStore(context).load());
    }
}
