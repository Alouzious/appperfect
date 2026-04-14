package com.example.pitchperfect.ui.feedback;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pitchperfect.R;
import com.example.pitchperfect.api.ApiClient;
import com.example.pitchperfect.databinding.ActivityFeedbackBinding;
import com.example.pitchperfect.models.PracticeFeedback;
import com.example.pitchperfect.ui.auth.LoginActivity;
import com.example.pitchperfect.ui.home.HomeActivity;
import com.example.pitchperfect.ui.practice.PracticeActivity;
import com.example.pitchperfect.utils.SessionManager;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedbackActivity extends AppCompatActivity {

    private ActivityFeedbackBinding binding;
    private SessionManager sessionManager;
    private String sessionId;
    private String deckId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFeedbackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        sessionId = getIntent().getStringExtra("session_id");
        deckId = getIntent().getStringExtra("deck_id");

        setupToolbar();
        setupClickListeners();

        if ("mock_session".equals(sessionId)) {
            loadMockFeedback();
        } else {
            loadFeedback();
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Your Feedback");
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_home) {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_practice_main) {
            if (deckId != null) {
                Intent intent = new Intent(this, PracticeActivity.class);
                intent.putExtra("deck_id", deckId);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            } else {
                finish(); // Just go back
            }
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        sessionManager.clearSession();
        ApiClient.resetClient();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupClickListeners() {
        binding.btnPracticeAgain.setOnClickListener(v -> {
            if (deckId != null) {
                Intent intent = new Intent(this, PracticeActivity.class);
                intent.putExtra("deck_id", deckId);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            } else {
                finish(); // Goes back to PracticeActivity
            }
        });
    }

    private void loadFeedback() {
        if (sessionId == null) {
            Toast.makeText(this, "Session ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.getClient(this).getPracticeFeedback(sessionId).enqueue(new Callback<PracticeFeedback>() {
            @Override
            public void onResponse(@NonNull Call<PracticeFeedback> call, @NonNull Response<PracticeFeedback> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayFeedback(response.body());
                } else {
                    Toast.makeText(FeedbackActivity.this, "Failed to load feedback: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<PracticeFeedback> call, @NonNull Throwable t) {
                Toast.makeText(FeedbackActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMockFeedback() {
        // Create mock data
        String json = "{" +
                "\"session_id\": \"mock_session\"," +
                "\"overall_score\": 85.0," +
                "\"scores\": {" +
                "  \"pace\": 90.0," +
                "  \"clarity\": 82.0," +
                "  \"confidence\": 88.0," +
                "  \"content\": 75.0," +
                "  \"structure\": 80.0" +
                "}," +
                "\"feedback\": \"Your pitch was very energetic and clear. You maintained good eye contact (simulated) and your pace was just right for an elevator pitch. Try to focus more on the problem statement at the beginning.\"," +
                "\"strengths\": [\"Excellent pace and energy\", \"Clear pronunciation\", \"Strong call to action\"]," +
                "\"improvements\": [\"Elaborate more on the unique value proposition\", \"Opening hook could be more engaging\"]," +
                "\"improvement_from_last\": 5.5" +
                "}";
        
        PracticeFeedback feedback = new Gson().fromJson(json, PracticeFeedback.class);
        displayFeedback(feedback);
        Toast.makeText(this, "Displaying Mock Data", Toast.LENGTH_SHORT).show();
    }

    private void displayFeedback(PracticeFeedback feedback) {
        // Overall score
        int overall = (int) feedback.getOverallScore();
        binding.tvOverallScore.setText(String.valueOf(overall));

        // Session info
        binding.tvSessionInfo.setText(String.format(Locale.getDefault(), "Score improved by %.1f points", feedback.getImprovementFromLast()));

        // Score breakdown
        if (feedback.getScores() != null) {
            PracticeFeedback.Scores scores = feedback.getScores();
            binding.progressPace.setProgress((int) scores.getPace());
            binding.tvPaceScore.setText(String.format(Locale.getDefault(), "%d/100", (int) scores.getPace()));

            binding.progressClarity.setProgress((int) scores.getClarity());
            binding.tvClarityScore.setText(String.format(Locale.getDefault(), "%d/100", (int) scores.getClarity()));

            binding.progressConfidence.setProgress((int) scores.getConfidence());
            binding.tvConfidenceScore.setText(String.format(Locale.getDefault(), "%d/100", (int) scores.getConfidence()));

            binding.progressContent.setProgress((int) scores.getContent());
            binding.tvContentScore.setText(String.format(Locale.getDefault(), "%d/100", (int) scores.getContent()));

            binding.progressStructure.setProgress((int) scores.getStructure());
            binding.tvStructureScore.setText(String.format(Locale.getDefault(), "%d/100", (int) scores.getStructure()));
        }

        // Feedback text
        binding.tvFeedback.setText(feedback.getFeedback());

        // Strengths
        if (feedback.getStrengths() != null) {
            StringBuilder sb = new StringBuilder();
            for (String s : feedback.getStrengths()) {
                sb.append("• ").append(s).append("\n");
            }
            binding.tvStrengths.setText(sb.toString().trim());
        }

        // Improvements
        if (feedback.getImprovements() != null) {
            StringBuilder sb = new StringBuilder();
            for (String s : feedback.getImprovements()) {
                sb.append("• ").append(s).append("\n");
            }
            binding.tvImprovements.setText(sb.toString().trim());
        }
    }
}
