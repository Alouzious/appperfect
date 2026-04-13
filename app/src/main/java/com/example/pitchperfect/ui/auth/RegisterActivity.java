package com.example.pitchperfect.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pitchperfect.api.ApiClient;
import com.example.pitchperfect.databinding.ActivityRegisterBinding;
import com.example.pitchperfect.models.CsrfResponse;
import com.example.pitchperfect.models.RegisterRequest;
import com.example.pitchperfect.models.RegisterResponse;
import com.example.pitchperfect.utils.SessionManager;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private SessionManager sessionManager;
    private String csrfToken = "";
    private String sessionCookie = "";
    private static final String REFERER_URL = "https://pitch-perfect-api.onrender.com/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        fetchCsrfToken();
        setupClickListeners();
    }

    private void fetchCsrfToken() {
        ApiClient.getClient().getCsrfToken().enqueue(new Callback<CsrfResponse>() {
            @Override
            public void onResponse(Call<CsrfResponse> call, Response<CsrfResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Start with the token from the response body
                    csrfToken = response.body().getCsrfToken();
                    
                    // Capture all cookies and refine the CSRF token if present in cookies
                    List<String> cookies = response.headers().values("Set-Cookie");
                    if (cookies != null && !cookies.isEmpty()) {
                        StringBuilder cookieBuilder = new StringBuilder();
                        for (String cookie : cookies) {
                            String cookiePart = cookie.split(";")[0];
                            if (cookieBuilder.length() > 0) {
                                cookieBuilder.append("; ");
                            }
                            cookieBuilder.append(cookiePart);
                            
                            // If the cookie contains the csrf token, use that one as it's more reliable for Django
                            if (cookiePart.startsWith("csrftoken=")) {
                                csrfToken = cookiePart.substring("csrftoken=".length());
                            }
                        }
                        sessionCookie = cookieBuilder.toString();
                    }
                }
            }
            @Override
            public void onFailure(Call<CsrfResponse> call, Throwable t) {}
        });
    }

    private void setupClickListeners() {
        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.tvLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String username = binding.etUsername.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        if (username.isEmpty()) { binding.etUsername.setError("Required"); return; }
        if (email.isEmpty()) { binding.etEmail.setError("Required"); return; }
        if (password.isEmpty()) { binding.etPassword.setError("Required"); return; }
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }
        if (password.length() < 8) {
            showError("Password must be at least 8 characters");
            return;
        }

        showLoading(true);
        hideError();

        ApiClient.getClient().register(csrfToken, sessionCookie, REFERER_URL, new RegisterRequest(username, email, password))
                .enqueue(new Callback<RegisterResponse>() {
                    @Override
                    public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                        showLoading(false);
                        if (response.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        } else {
                            handleErrorResponse(response);
                        }
                    }

                    @Override
                    public void onFailure(Call<RegisterResponse> call, Throwable t) {
                        showLoading(false);
                        showError("Network error: " + t.getMessage());
                    }
                });
    }

    private void handleErrorResponse(Response<RegisterResponse> response) {
        if (response.code() == 500) {
            showError("Server Error (500). The server is currently unable to handle this request. This often happens if the email service is not configured or there is a database issue.");
            return;
        }
        try {
            String errorBody = response.errorBody().string();
            JSONObject jsonObject = new JSONObject(errorBody);
            
            StringBuilder errorMsg = new StringBuilder();
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = jsonObject.get(key);
                if (value instanceof org.json.JSONArray) {
                    errorMsg.append(key).append(": ").append(((org.json.JSONArray) value).getString(0)).append("\n");
                } else {
                    errorMsg.append(value.toString()).append("\n");
                }
            }
            
            if (errorMsg.length() > 0) {
                showError(errorMsg.toString().trim());
            } else {
                showError("Registration failed (Code " + response.code() + ")");
            }
        } catch (Exception e) {
            showError("Registration failed. Error code: " + response.code());
        }
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!show);
    }

    private void showError(String message) {
        binding.tvError.setText(message);
        binding.tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        binding.tvError.setVisibility(View.GONE);
    }
}
