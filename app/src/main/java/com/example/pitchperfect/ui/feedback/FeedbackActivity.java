package com.example.pitchperfect.ui.feedback;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pitchperfect.api.ApiClient;
import com.example.pitchperfect.databinding.ActivityFeedbackBinding;
import com.example.pitchperfect.models.PracticeFeedback;
import com.example.pitchperfect.utils.SessionManager;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedbackActivity extends AppCompatActivity {

    private ActivityFeedbackBinding binding;
    private SessionManager sessionManager;
    private String sessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFeedbackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        sessionId = getIntent().getStringExtra("session_id");

        setupClickListeners();
        loadFeedback();
    }

    private void setupClickListeners() {
        binding.btnPracticeAgain.setOnClickListener(v -> {
            finish(); // Goes back to PracticeActivity
        });
    }

    private void loadFeedback() {
        ApiClient.getClient(this).getPracticeFeedback(sessionId).enqueue(new Callback<PracticeFeedback>() {
            @Override
            public void onResponse(@NonNull Call<PracticeFeedback> call, @NonNull Response<PracticeFeedback> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayFeedback(response.body());
                }
            }
            @Override
            public void onFailure(@NonNull Call<PracticeFeedback> call, @NonNull Throwable t) {}
        });
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
