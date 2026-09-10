package com.keepers.photoorganiser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StackPresentation {
    private StackPresentation() {}

    public static List<String> visibleIds(List<String> orderedIds,
            Map<String, List<String>> stacks, Set<String> recommended, Set<String> keepers) {
        ArrayList<String> visible = new ArrayList<>();
        Set<List<String>> handled = new HashSet<>();
        for (String id : orderedIds) {
            List<String> stack = stacks.get(id);
            if (stack == null) {
                visible.add(id);
                continue;
            }
            if (!handled.add(stack)) continue;
            String cover = stack.stream().filter(keepers::contains).findFirst()
                    .orElseGet(() -> stack.stream().filter(recommended::contains).findFirst()
                            .orElse(stack.get(0)));
            visible.add(cover);
        }
        return visible;
    }

    public static List<String> navigationIds(List<String> orderedIds,
            Map<String, List<String>> stacks, Set<String> recommended, Set<String> keepers,
            String currentId) {
        ArrayList<String> navigation = new ArrayList<>(
                visibleIds(orderedIds, stacks, recommended, keepers));
        List<String> currentStack = stacks.get(currentId);
        if (currentStack == null) return List.copyOf(navigation);
        for (int index = 0; index < navigation.size(); index++) {
            if (!currentStack.contains(navigation.get(index))) continue;
            navigation.set(index, currentId);
            break;
        }
        return List.copyOf(navigation);
    }
}
