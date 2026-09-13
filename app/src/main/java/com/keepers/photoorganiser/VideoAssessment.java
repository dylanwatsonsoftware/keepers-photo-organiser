package com.keepers.photoorganiser;

import java.util.List;
import java.util.stream.Collectors;

public record VideoAssessment(int score, int sampledFrames, List<AssessmentRule> rules) {
    public VideoAssessment {
        rules = List.copyOf(rules);
    }

    public static VideoAssessment from(VideoFeatures video) {
        return new VideoAssessment(video.score(), video.sampledFrames(), List.of(
                AssessmentRule.scored("Focus", video.focus(),
                        "Average centre-weighted clarity across sampled frames."),
                AssessmentRule.scored("Useful detail", video.detail(),
                        "Texture and edge information across the clip samples."),
                AssessmentRule.scored("Exposure quality", video.exposure(),
                        "Rewards usable mid-tones and penalises clipped sampled frames."),
                AssessmentRule.scored("Composition", video.composition(),
                        "Average visual balance across sampled moments."),
                AssessmentRule.scored("Frame stability", video.frameStability(),
                        "Estimates blur and directional smearing in sampled frames."),
                AssessmentRule.scored("Visible frames", 1 - video.blackFrameRate(),
                        "Penalises sampled moments that are almost entirely black."),
                AssessmentRule.scored("Changing scene", 1 - video.frozenFrameRate(),
                        "Checks whether adjacent sampled moments visibly change.")));
    }

    public String explanation() {
        return sampledFrames + (sampledFrames == 1 ? " sampled frame" : " sampled frames")
                + " across the clip\n\n" + rules.stream().map(AssessmentRule::display)
                .collect(Collectors.joining("\n\n"));
    }
}
