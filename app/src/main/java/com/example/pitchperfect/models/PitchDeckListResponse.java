package com.example.pitchperfect.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PitchDeckListResponse {
    @SerializedName("count")
    private int count;

    @SerializedName("results")
    private List<PitchDeck> results;

    public int getCount() { return count; }
    public List<PitchDeck> getResults() { return results; }
}