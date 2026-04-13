package com.example.pitchperfect.models;

import com.google.gson.annotations.SerializedName;

/**
 * Model class for CSRF Response
 */
public class CsrfResponse {
    @SerializedName("csrfToken")
    private String csrfToken;

    @SerializedName("csrf_token")
    private String csrfTokenAlt;

    @SerializedName("token")
    private String token;

    public String getCsrfToken() {
        if (csrfToken != null) return csrfToken;
        if (csrfTokenAlt != null) return csrfTokenAlt;
        return token;
    }
}
