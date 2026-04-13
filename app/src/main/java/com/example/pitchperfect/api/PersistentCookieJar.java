package com.example.pitchperfect.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * CookieJar implementation that persists cookies to SharedPreferences
 */
public class PersistentCookieJar implements CookieJar {
    private static final String COOKIE_PREFS = "PitchPerfectCookies";
    private final SharedPreferences sharedPreferences;

    public PersistentCookieJar(Context context) {
        this.sharedPreferences = context.getSharedPreferences(COOKIE_PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public void saveFromResponse(@NonNull HttpUrl url, @NonNull List<Cookie> cookies) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (Cookie cookie : cookies) {
            editor.putString(cookie.name(), serializeCookie(cookie));
        }
        editor.apply();
    }

    @NonNull
    @Override
    public List<Cookie> loadForRequest(@NonNull HttpUrl url) {
        List<Cookie> cookies = new ArrayList<>();
        for (String name : sharedPreferences.getAll().keySet()) {
            String serialized = sharedPreferences.getString(name, null);
            if (serialized != null) {
                Cookie cookie = deserializeCookie(name, serialized);
                if (cookie != null) {
                    // Only add if not expired and matches the requested URL (domain/path)
                    if (cookie.expiresAt() > System.currentTimeMillis() && cookie.matches(url)) {
                        cookies.add(cookie);
                    }
                }
            }
        }
        return cookies;
    }

    private String serializeCookie(Cookie cookie) {
        return cookie.value() + "|" + cookie.expiresAt() + "|" +
                cookie.domain() + "|" + cookie.path() + "|" +
                cookie.secure() + "|" + cookie.httpOnly();
    }

    private Cookie deserializeCookie(String name, String serialized) {
        try {
            String[] parts = serialized.split("\\|");
            if (parts.length >= 6) {
                Cookie.Builder builder = new Cookie.Builder()
                        .name(name)
                        .value(parts[0])
                        .expiresAt(Long.parseLong(parts[1]));

                String domain = parts[2];
                // Remove leading dot if present to avoid validation issues in some OkHttp versions
                if (domain.startsWith(".")) {
                    domain = domain.substring(1);
                }
                builder.domain(domain);

                builder.path(parts[3]);
                if (Boolean.parseBoolean(parts[4])) {
                    builder.secure();
                }
                if (Boolean.parseBoolean(parts[5])) {
                    builder.httpOnly();
                }
                return builder.build();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Helper to get a cookie value by name (e.g., "csrftoken")
     */
    public String getCookieValue(String name) {
        String serialized = sharedPreferences.getString(name, null);
        if (serialized != null) {
            Cookie cookie = deserializeCookie(name, serialized);
            if (cookie != null && cookie.expiresAt() > System.currentTimeMillis()) {
                return cookie.value();
            }
        }
        return null;
    }

    public void clearCookies() {
        sharedPreferences.edit().clear().apply();
    }
}
