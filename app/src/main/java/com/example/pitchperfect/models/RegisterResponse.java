package com.example.pitchperfect.models;

import com.google.gson.annotations.SerializedName;

public class RegisterResponse {
    @SerializedName("message")
    private String message;

    @SerializedName("user")
    private User user;

    public String getMessage() { return message; }
    public User getUser() { return user; }
}