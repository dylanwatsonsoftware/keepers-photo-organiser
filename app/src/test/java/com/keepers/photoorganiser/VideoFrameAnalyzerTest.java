package com.keepers.photoorganiser;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

public class VideoFrameAnalyzerTest {
    @Test public void samplesOnlyThreeBoundedMomentsAcrossTheClip() {
        assertEquals(List.of(1_500_000L, 5_000_000L, 8_500_000L),
                VideoFrameAnalyzer.sampleTimesMicros(10_000));
    }
}
