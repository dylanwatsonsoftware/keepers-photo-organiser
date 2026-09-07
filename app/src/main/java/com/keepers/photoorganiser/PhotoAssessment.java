package com.keepers.photoorganiser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record PhotoAssessment(int score, List<AssessmentRule> rules) {
    public PhotoAssessment {
        rules = List.copyOf(rules);
    }

    public static PhotoAssessment from(PhotoFeatures photo, PhotoStackPosition stack,
            boolean previousKeeper) {
        ArrayList<AssessmentRule> rules = new ArrayList<>();
        rules.add(AssessmentRule.scored("Focus", photo.focus(),
                "Edge clarity, weighted toward the centre of the photo."));
        rules.add(AssessmentRule.scored("Useful detail", photo.quality(),
                "Texture and edge information present in the image."));
        rules.add(AssessmentRule.pending("Smiles and expressions",
                "Requires face analysis."));
        rules.add(AssessmentRule.pending("Closed eyes", "Requires face analysis."));
        rules.add(AssessmentRule.pending("Every child looks good",
                "Requires child profiles plus face and expression analysis."));
        rules.add(AssessmentRule.pending("Action or emotional significance",
                "Requires scene understanding and your feedback."));
        rules.add(AssessmentRule.scored("Composition", photo.composition(),
                "Visual balance and subject placement around the thirds."));
        rules.add(AssessmentRule.scored("Exposure quality", photo.exposure(),
                "Rewards usable mid-tones and penalises clipped shadows and highlights."));
        rules.add(AssessmentRule.scored("Motion blur", photo.motionStability(),
                "Estimates directional smearing separately from useful image detail."));
        rules.add(AssessmentRule.scored("Variety among selected moments",
                stack == null ? 1.0 : 1.0 / stack.size(),
                stack == null ? "A visually distinct moment."
                        : "Part of a " + stack.size() + "-photo moment; normally only its best shot is selected."));
        rules.add(AssessmentRule.scored("Your previous Keeper choices", previousKeeper ? 1 : 0,
                previousKeeper ? "You previously marked this photo as a Keeper."
                        : "No matching Keeper preference has been learned for this photo yet."));
        double core = (photo.focus() + photo.exposure() + photo.composition()
                + photo.motionStability() + photo.quality()) / 5d;
        return new PhotoAssessment((int) Math.round(core * 100), rules);
    }

    public String explanation() {
        return rules.stream().map(AssessmentRule::display).collect(Collectors.joining("\n\n"));
    }
}
