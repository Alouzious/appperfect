package com.example.pitchperfect.utils;

import com.example.pitchperfect.data.MockDataRepository;
import com.example.pitchperfect.models.PracticeFeedback;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DemoFeedbackGenerator {

    public static PracticeFeedback generateDemoFeedback(String pitchType) {
        PracticeFeedback feedback = new PracticeFeedback();

        // Set basic fields
        feedback.setStatus("completed");
        feedback.setId("demo_" + System.currentTimeMillis());

        // Generate scores based on pitch type
        double[] scores = MockDataRepository.MockScores.generateScores();
        
        PracticeFeedback.Scores scoreObj = new PracticeFeedback.Scores();
        scoreObj.setPace(scores[0]);
        scoreObj.setClarity(scores[1]);
        scoreObj.setConfidence(scores[2]);
        scoreObj.setContent(scores[3]);
        scoreObj.setStructure(scores[4]);
        feedback.setScores(scoreObj);

        // Calculate overall score
        double overall = (scores[0] + scores[1] + scores[2] + scores[3] + scores[4]) / 5.0;
        feedback.setOverallScore(overall);
        feedback.setImprovementFromLast(generateRandomScore(5, 15));

        // Generate contextual feedback
        String feedbackText = generateFeedbackText(pitchType, scoreObj);
        feedback.setFeedback(feedbackText);

        // Generate strengths and improvements from mock data
        feedback.setStrengths(getRandomStrengths(4));
        feedback.setImprovements(getRandomImprovements(4));

        return feedback;
    }

    private static String generateFeedbackText(String pitchType, PracticeFeedback.Scores scores) {
        StringBuilder feedback = new StringBuilder();

        feedback.append("Excellent delivery on your pitch! ");

        if (scores.getClarity() > 75) {
            feedback.append("Your message was clear and well-articulated. ");
        } else {
            feedback.append("Work on enunciating key points with more clarity. ");
        }

        if (scores.getConfidence() > 80) {
            feedback.append("You demonstrated strong confidence throughout the presentation. ");
        } else {
            feedback.append("Building confidence will strengthen your delivery significantly. ");
        }

        feedback.append("Your structure was logical and easy to follow. ");
        feedback.append("Keep practicing to achieve even better results!");

        return feedback.toString();
    }

    private static List<String> getRandomStrengths(int count) {
        List<String> strengths = new ArrayList<>(
            Arrays.asList(MockDataRepository.MockScores.STRENGTH_EXAMPLES)
        );
        Collections.shuffle(strengths);
        return new ArrayList<>(strengths.subList(0, Math.min(count, strengths.size())));
    }

    private static List<String> getRandomImprovements(int count) {
        List<String> improvements = new ArrayList<>(
            Arrays.asList(MockDataRepository.MockScores.IMPROVEMENT_EXAMPLES)
        );
        Collections.shuffle(improvements);
        return new ArrayList<>(improvements.subList(0, Math.min(count, improvements.size())));
    }

    private static double generateRandomScore(int min, int max) {
        return min + Math.random() * (max - min);
    }
}
