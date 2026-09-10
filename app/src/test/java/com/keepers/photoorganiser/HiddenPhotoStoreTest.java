package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.Set;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class HiddenPhotoStoreTest {
    @Test public void hidesRestoresAndClearsPhotos() {
        HiddenPhotoStore store = new HiddenPhotoStore(RuntimeEnvironment.getApplication());
        store.hide(Set.of("a", "b"));
        store.restore("a");
        assertEquals(Set.of("b"), store.load());
        store.clear();
        assertEquals(Set.of(), store.load());
    }

    @Test public void hidingOneStackMemberHidesTheWholeStack() {
        String first = "stack-first";
        String second = "stack-second";
        String third = "stack-third";
        List<String> stack = List.of(first, second, third);
        new PhotoStackStore(RuntimeEnvironment.getApplication()).save(Map.of(
                first, stack, second, stack, third, stack));
        HiddenPhotoStore store = new HiddenPhotoStore(RuntimeEnvironment.getApplication());

        store.hide(Set.of(second));

        assertEquals(Set.of(first, second, third), store.load());
    }
}
