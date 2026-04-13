package com.example.pitchperfect.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PracticeFeedback {
    @SerializedName("session_id")
    private String sessionId;

    @SerializedName("overall_score")
    private float overallScore;

    @SerializedName("scores")
    private Scores scores;

    @SerializedName("metrics")
    private Metrics metrics;

    @SerializedName("feedback")
    private String feedback;

    @SerializedName("strengths")
    private List<String> strengths;

    @SerializedName("improvements")
    private List<String> improvements;

    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("improvement_from_last")
    private float improvementFromLast;

    public String getSessionId() { return sessionId; }
    public float getOverallScore() { return overallScore; }
    public Scores getScores() { return scores; }
    public Metrics getMetrics() { return metrics; }
    public String getFeedback() { return feedback; }
    public List<String> getStrengths() { return strengths; }
    public List<String> getImprovements() { return improvements; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public float getImprovementFromLast() { return improvementFromLast; }

    public static class Scores {
        @SerializedName("pace") private float pace;
        @SerializedName("clarity") private float clarity;
        @SerializedName("confidence") private float confidence;
        @SerializedName("content") private float content;
        @SerializedName("structure") private float structure;

        public float getPace() { return pace; }
        public float getClarity() { return clarity; }
        public float getConfidence() { return confidence; }
        public float getContent() { return content; }
        public float getStructure() { return structure; }
    }

    public static class Metrics {
        @SerializedName("duration_seconds") private int durationSeconds;
        @SerializedName("word_count") private int wordCount;
        @SerializedName("speaking_pace_wpm") private float speakingPaceWpm;
        @SerializedName("filler_words_count") private int fillerWordsCount;

        public int getDurationSeconds() { return durationSeconds; }
        public int getWordCount() { return wordCount; }
        public float getSpeakingPaceWpm() { return speakingPaceWpm; }
        public int getFillerWordsCount() { return fillerWordsCount; }
    }
}