package com.example.pitchperfect.ui.feedback;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pitchperfect.api.ApiClient;
import com.example.pitchperfect.databinding.ActivityFeedbackBinding;
import com.example.pitchperfect.models.PracticeFeedback;
import com.example.pitchperfect.utils.SessionManager;
import com.example.pitchperfect.utils.TextToSpeechHelper;

import java.util.Arrays;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FeedbackActivity extends AppCompatActivity {

    private ActivityFeedbackBinding binding;
    private SessionManager sessionManager;
    private String sessionId;
    private TextToSpeechHelper ttsHelper;
    private PracticeFeedback currentFeedback;
    private boolean isDemo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFeedbackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        sessionId = getIntent().getStringExtra("session_id");
        isDemo = getIntent().getBooleanExtra("demo_feedback", false);
        ttsHelper = new TextToSpeechHelper(this);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Your Feedback");
        }

        setupClickListeners();

        if (isDemo) {
            loadDemoFeedback();
        } else {
            loadFeedback();
        }
    }

    private void loadDemoFeedback() {
        double score = getIntent().getDoubleExtra("feedback_score", 0);
        String feedbackText = getIntent().getStringExtra("feedback_text");

        PracticeFeedback feedback = new PracticeFeedback();
        feedback.setOverallScore(score);
        feedback.setFeedback(feedbackText);
        feedback.setImprovementFromLast(8.5);

        PracticeFeedback.Scores scores = new PracticeFeedback.Scores();
        scores.setPace(78);
        scores.setClarity(82);
        scores.setConfidence(85);
        scores.setContent(80);
        scores.setStructure(79);
        feedback.setScores(scores);

        feedback.setStrengths(Arrays.asList(
                "Clear and concise messaging",
                "Good pacing and delivery",
                "Well-structured pitch flow"
        ));
        feedback.setImprovements(Arrays.asList(
                "Add more specific examples",
                "Emphasize key differentiators",
                "Practice smoother transitions"
        ));

        currentFeedback = feedback;
        displayFeedback(feedback);
    }

    private void setupClickListeners() {
        binding.btnPracticeAgain.setOnClickListener(v -> {
            finish(); // Goes back to PracticeActivity
        });

        binding.btnReadFeedback.setOnClickListener(v -> readFeedbackAloud());
        binding.btnStopSpeaking.setOnClickListener(v -> {
            ttsHelper.stop();
            binding.btnReadFeedback.setVisibility(View.VISIBLE);
            binding.btnStopSpeaking.setVisibility(View.GONE);
        });
    }

    private void readFeedbackAloud() {
        if (currentFeedback == null) return;

        StringBuilder feedbackText = new StringBuilder();
        feedbackText.append("Overall score: ").append((int) currentFeedback.getOverallScore()).append(" out of 100. ");
        feedbackText.append(currentFeedback.getFeedback()).append(". ");

        if (currentFeedback.getStrengths() != null && !currentFeedback.getStrengths().isEmpty()) {
            feedbackText.append("Your strengths: ");
            for (String s : currentFeedback.getStrengths()) {
                feedbackText.append(s).append(". ");
            }
        }

        if (currentFeedback.getImprovements() != null && !currentFeedback.getImprovements().isEmpty()) {
            feedbackText.append("Areas to improve: ");
            for (String s : currentFeedback.getImprovements()) {
                feedbackText.append(s).append(". ");
            }
        }

        ttsHelper.speak(feedbackText.toString());
        binding.btnReadFeedback.setVisibility(View.GONE);
        binding.btnStopSpeaking.setVisibility(View.VISIBLE);
    }

    private void loadFeedback() {
        ApiClient.getClient().getPracticeFeedback(
                sessionManager.getSessionCookie(), sessionId
        ).enqueue(new Callback<PracticeFeedback>() {
            @Override
            public void onResponse(Call<PracticeFeedback> call, Response<PracticeFeedback> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentFeedback = response.body();
                    displayFeedback(response.body());
                }
            }
            @Override
            public void onFailure(Call<PracticeFeedback> call, Throwable t) {}
        });
    }

    private void displayFeedback(PracticeFeedback feedback) {
        // Overall score
        int overall = (int) feedback.getOverallScore();
        binding.tvOverallScore.setText(String.valueOf(overall));

        // Session info
        binding.tvSessionInfo.setText(String.format(Locale.getDefault(), "Improvement: +%.1f points", feedback.getImprovementFromLast()));

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsHelper != null) {
            ttsHelper.release();
        }
    }
}
