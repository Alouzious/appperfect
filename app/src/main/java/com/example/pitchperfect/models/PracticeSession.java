package com.example.pitchperfect.models;

import com.google.gson.annotations.SerializedName;

public class PracticeSession {
    @SerializedName("id")
    private String id;

    @SerializedName("pitch_deck")
    private String pitchDeck;

    @SerializedName("pitch_deck_title")
    private String pitchDeckTitle;

    @SerializedName("pitch_type")
    private String pitchType;

    @SerializedName("session_number")
    private int sessionNumber;

    @SerializedName("overall_score")
    private float overallScore;

    @SerializedName("status")
    private String status;

    @SerializedName("transcript")
    private String transcript;

    @SerializedName("duration_seconds")
    private int durationSeconds;

    @SerializedName("created_at")
    private String createdAt;

    // submit-audio response fields
    @SerializedName("message")
    private String message;

    @SerializedName("session_id")
    private String sessionId;

    @SerializedName("transcript_preview")
    private String transcriptPreview;

    @SerializedName("word_count")
    private int wordCount;

    public String getId() { return id; }
    public String getPitchDeck() { return pitchDeck; }
    public String getPitchDeckTitle() { return pitchDeckTitle; }
    public String getPitchType() { return pitchType; }
    public int getSessionNumber() { return sessionNumber; }
    public float getOverallScore() { return overallScore; }
    public String getStatus() { return status; }
    public String getTranscript() { return transcript; }
    public int getDurationSeconds() { return durationSeconds; }
    public String getCreatedAt() { return createdAt; }
    public String getMessage() { return message; }
    public String getSessionId() { return sessionId != null ? sessionId : id; }
    public String getTranscriptPreview() { return transcriptPreview; }
    public int getWordCount() { return wordCount; }
}