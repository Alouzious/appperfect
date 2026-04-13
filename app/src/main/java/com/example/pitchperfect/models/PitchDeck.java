package com.example.pitchperfect.models;

import com.google.gson.annotations.SerializedName;

public class PitchDeck {
    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("total_slides")
    private int totalSlides;

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("file_type")
    private String fileType;

    public String getId() { return id; }
    public String getTitle() { return title; }
    public int getTotalSlides() { return totalSlides; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getFileType() { return fileType; }
}