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

    @SerializedName("re_password")
    private String rePassword;

    public RegisterRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.rePassword = password;
    }
}
