package com.example.pitchperfect.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pitchperfect.api.ApiClient;
import com.example.pitchperfect.databinding.ActivityLoginBinding;
import com.example.pitchperfect.models.CsrfResponse;
import com.example.pitchperfect.models.LoginRequest;
import com.example.pitchperfect.models.LoginResponse;
import com.example.pitchperfect.ui.home.HomeActivity;
import com.example.pitchperfect.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private SessionManager sessionManager;
    private String csrfToken = "";
    private static final String BASE_URL = "https://pitch-perfect-api.onrender.com/api/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        // If already logged in go straight to home
        if (sessionManager.isLoggedIn()) {
            goToHome();
            return;
        }

        fetchCsrfToken();
        setupClickListeners();
    }

    private void fetchCsrfToken() {
        // Fetch CSRF token - this also initializes the session cookie
        ApiClient.getClient(this).getCsrfToken().enqueue(new Callback<CsrfResponse>() {
            @Override
            public void onResponse(Call<CsrfResponse> call, Response<CsrfResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    csrfToken = response.body().getCsrfToken();
                }
            }

            @Override
            public void onFailure(Call<CsrfResponse> call, Throwable t) {
                // Log error but continue - CSRF can be retried
                t.printStackTrace();
            }
        });
    }

    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void attemptLogin() {
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (username.isEmpty()) {
            binding.etUsername.setError("Username is required");
            return;
        }
        if (password.isEmpty()) {
            binding.etPassword.setError("Password is required");
            return;
        }

        showLoading(true);
        hideError();

        // OkHttp now automatically handles cookies via cookieJar
        // Just pass CSRF token, cookie header (empty - handled by OkHttp), and referer
        ApiClient.getClient(this).login(
                csrfToken,
                "", // Cookie header is handled automatically by OkHttp
                BASE_URL,
                new LoginRequest(username, password)
        ).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    sessionManager.saveSession(
                            loginResponse.getUser().getUsername(),
                            loginResponse.getUser().getEmail(),
                            loginResponse.getUser().getId()
                    );
                    goToHome();
                } else {
                    showError("Invalid username or password");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                showLoading(false);
                showError("Network error: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }

    private void goToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!show);
    }

    private void showError(String message) {
        binding.tvError.setText(message);
        binding.tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        binding.tvError.setVisibility(View.GONE);
    }
}
