package com.example.pitchperfect.models;

import com.google.gson.annotations.SerializedName;

public class PracticeSessionRequest {
    @SerializedName("pitch_deck")
    private String pitchDeck;

    @SerializedName("pitch_type")
    private String pitchType;

    @SerializedName("transcript")
    private String transcript;

    @SerializedName("duration_seconds")
    private int durationSeconds;

    @SerializedName("target_duration_seconds")
    private int targetDurationSeconds;

    public PracticeSessionRequest(String pitchDeck, String pitchType, String transcript, int durationSeconds, int targetDurationSeconds) {
        this.pitchDeck = pitchDeck;
        this.pitchType = pitchType;
        this.transcript = transcript;
        this.durationSeconds = durationSeconds;
        this.targetDurationSeconds = targetDurationSeconds;
    }
}