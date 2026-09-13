package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PhotoContextStoreTest {
    @Test public void persistsVersionedDerivedContextProbabilities() {
        Context context = RuntimeEnvironment.getApplication();
        PhotoContextStore store = new PhotoContextStore(context);
        store.clear();
        PhotoContext expected = new PhotoContext(Map.of(
                PhotoContextType.PORTRAIT, .85,
                PhotoContextType.PET, .72,
                PhotoContextType.LOW_LIGHT, .4));

        store.save("photo", expected);

        PhotoContext loaded = store.load("photo");
        assertEquals(expected.probabilities(), loaded.probabilities());
        store.clear();
    }
}
