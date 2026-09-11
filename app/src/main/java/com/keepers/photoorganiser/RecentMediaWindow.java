package com.keepers.photoorganiser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

public final class RecentMediaWindow {
    public record Item(RecentPhoto media, PhotoOrigin origin) {}
    public record Result(List<Item> items, boolean hasMore) {}

    private RecentMediaWindow() {}

    public static Result combine(List<RecentPhoto> local, List<ImportedPhoto> imported, int limit) {
        LinkedHashMap<String, Item> byId = new LinkedHashMap<>();
        for (RecentPhoto media : local)
            byId.put(media.uri().toString(), new Item(media, PhotoOrigin.LOCAL));
        for (ImportedPhoto media : imported) byId.putIfAbsent(media.uri().toString(),
                new Item(new RecentPhoto(media.uri(), media.takenAtMillis(), media.mediaType(),
                        media.durationMillis()), media.origin()));
        ArrayList<Item> ordered = new ArrayList<>(byId.values());
        ordered.sort(Comparator.comparingLong(
                (Item item) -> item.media().takenAtMillis()).reversed());
        boolean hasMore = ordered.size() > limit;
        if (hasMore) ordered.subList(limit, ordered.size()).clear();
        return new Result(List.copyOf(ordered), hasMore);
    }
}
