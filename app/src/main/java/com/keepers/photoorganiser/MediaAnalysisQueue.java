package com.keepers.photoorganiser;

import java.util.ArrayDeque;
import java.util.List;

public final class MediaAnalysisQueue {
    private final ArrayDeque<RecentPhoto> videos = new ArrayDeque<>();
    private int photosRemaining;

    public void clear() {
        videos.clear();
        photosRemaining = 0;
    }

    public void add(List<RecentPhoto> media) {
        for (RecentPhoto item : media) {
            if (item.mediaType() == MediaType.VIDEO) videos.addLast(item);
            else photosRemaining++;
        }
    }

    public boolean photoCompleted() {
        if (photosRemaining <= 0) return false;
        photosRemaining--;
        return photosRemaining == 0;
    }

    public RecentPhoto pollVideo() {
        return photosRemaining == 0 ? videos.pollFirst() : null;
    }

    public boolean photosComplete() { return photosRemaining == 0; }
    public int pendingVideos() { return videos.size(); }
}
