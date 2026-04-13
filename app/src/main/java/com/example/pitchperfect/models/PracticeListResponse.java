package com.example.pitchperfect.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PracticeListResponse {
    @SerializedName("count")
    private int count;

    @SerializedName("results")
    private List<PracticeSession> results;

    public int getCount() { return count; }
    public List<PracticeSession> getResults() { return results; }
}