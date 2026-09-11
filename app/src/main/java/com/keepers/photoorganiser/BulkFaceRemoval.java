package com.keepers.photoorganiser;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class BulkFaceRemoval {
    private BulkFaceRemoval() {}

    public static Map<String, String> apply(String personId, Set<String> selectedFaceKeys,
            Map<String, String> corrections) {
        HashMap<String, String> changed = new HashMap<>(corrections);
        for (String key : selectedFaceKeys)
            if (personId.equals(changed.get(key))) changed.put(key, FaceCorrectionStore.IGNORE);
        return Map.copyOf(changed);
    }
}
