package com.example.pitchperfect.models;

import com.google.gson.annotations.SerializedName;

/**
 * Model class for Register Request
 */
public class RegisterRequest {
    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("password_confirm")
    private String passwordConfirm;

    public RegisterRequest(String username, String email, String password, String passwordConfirm) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.passwordConfirm = passwordConfirm;
    }
}
