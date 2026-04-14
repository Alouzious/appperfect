package com.example.pitchperfect.ui.practice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.pitchperfect.api.ApiClient;
import com.example.pitchperfect.databinding.ActivityPracticeBinding;
import com.example.pitchperfect.models.CsrfResponse;
import com.example.pitchperfect.models.PracticeFeedback;
import com.example.pitchperfect.models.PracticeListResponse;
import com.example.pitchperfect.models.PracticeSession;
import com.example.pitchperfect.models.PracticeSessionRequest;
import com.example.pitchperfect.ui.feedback.FeedbackActivity;
import com.example.pitchperfect.utils.SessionManager;
import com.example.pitchperfect.utils.TextToSpeechHelper;
import com.example.pitchperfect.utils.DemoFeedbackGenerator;
import com.example.pitchperfect.utils.SpeechRecognitionHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PracticeActivity extends AppCompatActivity {

    private ActivityPracticeBinding binding;
    private SessionManager sessionManager;
    private SessionAdapter sessionAdapter;
    private List<PracticeSession> sessionList = new ArrayList<>();

    private String deckId;
    private String deckTitle;
    private String csrfToken = "";
    private boolean csrfFetchInProgress = false;
    private boolean isDemo = false;
    private String demoDescription = "";
    private TextToSpeechHelper ttsHelper;
    private SpeechRecognitionHelper speechHelper;

    private MediaRecorder mediaRecorder;
    private File audioFile;
    private boolean isRecording = false;
    private int secondsElapsed = 0;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private static final int PERMISSION_REQUEST_CODE = 100;

    private static final String[] PITCH_TYPES = {
            "elevator", "demo_day", "investor", "competition"
    };
    private static final String[] PITCH_TYPE_LABELS = {
            "Elevator Pitch (30 sec)", "Demo Day (3 min)",
            "Investor Pitch (10 min)", "Competition Pitch (5 min)"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPracticeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        deckId = getIntent().getStringExtra("deck_id");
        deckTitle = getIntent().getStringExtra("deck_title");
        isDemo = getIntent().getBooleanExtra("is_demo", false);
        demoDescription = getIntent().getStringExtra("demo_description");

        ttsHelper = new TextToSpeechHelper(this);
        speechHelper = new SpeechRecognitionHelper(this);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Practice");
        }

        String displayTitle = deckTitle != null ? deckTitle : "Practice Your Pitch";
        if (isDemo) {
            displayTitle = "Demo: " + displayTitle;
        }
        binding.tvDeckTitle.setText(displayTitle);

        setupSpinner();
        setupRecyclerView();
        setupClickListeners();
        fetchCsrfToken();
        loadPreviousSessions();
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, PITCH_TYPE_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPitchType.setAdapter(adapter);
    }

    private void setupRecyclerView() {
        sessionAdapter = new SessionAdapter(this, sessionList);
        binding.rvSessions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSessions.setAdapter(sessionAdapter);
    }

    private void setupClickListeners() {
        binding.btnRecord.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
            } else {
                startRecording();
            }
        });

        binding.btnStop.setOnClickListener(v -> stopRecordingAndSubmit());
    }

    private void fetchCsrfToken() {
        if (csrfFetchInProgress) return;
        csrfFetchInProgress = true;
        ApiClient.getClient().getCsrfToken().enqueue(new Callback<CsrfResponse>() {
            @Override
            public void onResponse(Call<CsrfResponse> call,
                                   Response<CsrfResponse> response) {
                csrfFetchInProgress = false;
                if (response.isSuccessful() && response.body() != null) {
                    csrfToken = response.body().getCsrfToken();
                }
                storeCookiesFromResponse(response);
            }
            @Override
            public void onFailure(Call<CsrfResponse> call, Throwable t) {
                csrfFetchInProgress = false;
            }
        });
    }

    private void startRecording() {
        try {
            audioFile = new File(getCacheDir(), "practice_audio.m4a");
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            secondsElapsed = 0;
            startTimer();

            binding.btnRecord.setEnabled(false);
            binding.btnStop.setEnabled(true);
            binding.tvRecordingStatus.setText("Recording...");
            binding.tvRecordingStatus.setTextColor(0xFFDC3545);

        } catch (Exception e) {
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecordingAndSubmit() {
        if (!isRecording) return;

        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            stopTimer();

            binding.btnStop.setEnabled(false);
            binding.tvRecordingStatus.setText("Processing...");
            binding.layoutProcessing.setVisibility(View.VISIBLE);
            binding.tvProcessingStatus.setText("Transcribing your pitch...");

            submitAudio();

        } catch (Exception e) {
            Toast.makeText(this, "Recording error", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitAudio() {
        int selectedIndex = binding.spinnerPitchType.getSelectedItemPosition();
        String pitchType = PITCH_TYPES[selectedIndex];

        if (isDemo) {
            // For demo mode, skip API call and generate demo feedback
            binding.tvProcessingStatus.setText("Analyzing with AI... This may take 10-15 seconds");
            binding.layoutProcessing.setVisibility(View.VISIBLE);
            pollForDemoFeedback(pitchType, 0);
            return;
        }

        // First create a session
        PracticeSessionRequest request = new PracticeSessionRequest(
                deckId, pitchType, "pending", secondsElapsed, getTargetDuration(pitchType)
        );

        ApiClient.getClient().createPracticeSession(
                csrfToken, sessionManager.getSessionCookie(), request
        ).enqueue(new Callback<PracticeSession>() {
            @Override
            public void onResponse(Call<PracticeSession> call, Response<PracticeSession> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String sessionId = response.body().getId();
                    binding.tvProcessingStatus.setText("Sending audio to AI...");
                    submitAudioToSession(sessionId);
                } else {
                    showError("Failed to create session (" + response.code() + ")");
                }
            }
            @Override
            public void onFailure(Call<PracticeSession> call, Throwable t) {
                showError("Network error");
            }
        });
    }

    private void pollForDemoFeedback(String pitchType, int attempts) {
        if (attempts > 5) {
            // Generate and show demo feedback
            PracticeFeedback feedback = DemoFeedbackGenerator.generateDemoFeedback(pitchType);
            displayDemoFeedback(feedback);
            return;
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            pollForDemoFeedback(pitchType, attempts + 1);
        }, 3000);
    }

    private void displayDemoFeedback(PracticeFeedback feedback) {
        binding.layoutProcessing.setVisibility(View.GONE);

        // Show beautiful response modal with AI feedback
        showFeedbackModal(feedback);

        // Play response using TTS
        ttsHelper.speak(generateSpeechText(feedback));
    }

    private String generateSpeechText(PracticeFeedback feedback) {
        StringBuilder speech = new StringBuilder();
        speech.append("Your overall score is ").append((int) feedback.getOverallScore()).append(" out of 100. ");
        speech.append(feedback.getFeedback());
        return speech.toString();
    }

    private void showFeedbackModal(PracticeFeedback feedback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎉 Analysis Complete!");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_feedback_preview, null);
        builder.setView(dialogView);

        builder.setPositiveButton("View Full Feedback", (dialog, which) -> {
            // Store feedback and show full feedback screen
            Intent intent = new Intent(PracticeActivity.this, FeedbackActivity.class);
            intent.putExtra("demo_feedback", true);
            intent.putExtra("feedback_score", feedback.getOverallScore());
            intent.putExtra("feedback_text", feedback.getFeedback());
            startActivity(intent);
        });

        builder.setNegativeButton("Practice Again", (dialog, which) -> {
            dialog.dismiss();
            resetRecording();
        });

        builder.show();
    }

    private void submitAudioToSession(String sessionId) {
        RequestBody requestFile = RequestBody.create(MediaType.parse("audio/m4a"), audioFile);
        MultipartBody.Part audioPart = MultipartBody.Part.createFormData("audio", "practice.m4a", requestFile);
        RequestBody duration = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(secondsElapsed));

        ApiClient.getClient().submitPracticeAudio(
                csrfToken, sessionManager.getSessionCookie(), sessionId, audioPart, duration
        ).enqueue(new Callback<PracticeSession>() {
            @Override
            public void onResponse(Call<PracticeSession> call, Response<PracticeSession> response) {
                if (response.isSuccessful()) {
                    binding.tvProcessingStatus.setText("Analyzing with AI... This may take 10-15 seconds");
                    pollForFeedback(sessionId, 0);
                } else {
                    String backendError = readErrorBody(response);
                    String message = "Failed to submit audio (" + response.code() + ")";
                    if (backendError != null && !backendError.isEmpty()) {
                        message = message + ": " + backendError;
                    }
                    showError(message);
                }
            }
            @Override
            public void onFailure(Call<PracticeSession> call, Throwable t) {
                showError("Network error submitting audio");
            }
        });
    }

    private void pollForFeedback(String sessionId, int attempts) {
        if (attempts > 20) {
            showError("Analysis is taking long. Check back later.");
            return;
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ApiClient.getClient().getPracticeFeedback(
                    sessionManager.getSessionCookie(), sessionId
            ).enqueue(new Callback<PracticeFeedback>() {
                @Override
                public void onResponse(Call<PracticeFeedback> call, Response<PracticeFeedback> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        PracticeFeedback feedback = response.body();
                        if ("completed".equals(feedback.getStatus())) {
                            // Go to feedback screen
                            binding.layoutProcessing.setVisibility(View.GONE);
                            Intent intent = new Intent(PracticeActivity.this, FeedbackActivity.class);
                            intent.putExtra("session_id", sessionId);
                            intent.putExtra("deck_id", deckId);
                            startActivity(intent);
                            loadPreviousSessions();
                        } else {
                            // Still processing — poll again
                            pollForFeedback(sessionId, attempts + 1);
                        }
                    } else {
                        pollForFeedback(sessionId, attempts + 1);
                    }
                }
                @Override
                public void onFailure(Call<PracticeFeedback> call, Throwable t) {
                    pollForFeedback(sessionId, attempts + 1);
                }
            });
        }, 3000); // poll every 3 seconds
    }

    private void loadPreviousSessions() {
        ApiClient.getClient().getPracticeSessions(
                sessionManager.getSessionCookie(), deckId
        ).enqueue(new Callback<PracticeListResponse>() {
            @Override
            public void onResponse(Call<PracticeListResponse> call, Response<PracticeListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sessionList.clear();
                    sessionList.addAll(response.body().getResults());
                    sessionAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<PracticeListResponse> call, Throwable t) {}
        });
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                secondsElapsed++;
                int minutes = secondsElapsed / 60;
                int seconds = secondsElapsed % 60;
                binding.tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
    }

    private int getTargetDuration(String pitchType) {
        switch (pitchType) {
            case "elevator": return 30;
            case "demo_day": return 180;
            case "investor": return 600;
            case "competition": return 300;
            default: return 60;
        }
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            binding.layoutProcessing.setVisibility(View.GONE);
            binding.btnRecord.setEnabled(true);
            binding.btnStop.setEnabled(false);
            binding.tvRecordingStatus.setText("Ready to record");
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void storeCookiesFromResponse(Response<?> response) {
        List<String> cookies = response.headers().values("Set-Cookie");
        if (cookies == null || cookies.isEmpty()) return;

        String existing = sessionManager.getSessionCookie();
        String merged = mergeCookies(existing, cookies);
        sessionManager.saveSessionCookie(merged);
    }

    private String mergeCookies(String existingCookieHeader, List<String> setCookieHeaders) {
        Map<String, String> cookieMap = new LinkedHashMap<>();

        if (existingCookieHeader != null && !existingCookieHeader.trim().isEmpty()) {
            String[] existingPairs = existingCookieHeader.split(";");
            for (String pair : existingPairs) {
                String trimmed = pair.trim();
                if (trimmed.isEmpty() || !trimmed.contains("=")) continue;
                int idx = trimmed.indexOf('=');
                cookieMap.put(trimmed.substring(0, idx).trim(), trimmed.substring(idx + 1).trim());
            }
        }

        for (String header : setCookieHeaders) {
            if (header == null || header.trim().isEmpty()) continue;
            String firstPart = header.split(";")[0].trim();
            if (!firstPart.contains("=")) continue;
            int idx = firstPart.indexOf('=');
            cookieMap.put(firstPart.substring(0, idx).trim(), firstPart.substring(idx + 1).trim());
        }

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : cookieMap.entrySet()) {
            builder.append(entry.getKey()).append("=").append(entry.getValue()).append("; ");
        }
        return builder.toString();
    }

    private String readErrorBody(Response<?> response) {
        try {
            if (response.errorBody() == null) return "";
            String raw = response.errorBody().string();
            return raw.length() > 160 ? raw.substring(0, 160) + "..." : raw;
        } catch (IOException e) {
            return "";
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecording();
        } else {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (ttsHelper != null) {
            ttsHelper.release();
        }
        if (speechHelper != null) {
            speechHelper.release();
        }
    }

    private void resetRecording() {
        binding.btnRecord.setEnabled(true);
        binding.btnStop.setEnabled(false);
        binding.tvRecordingStatus.setText("Ready to record");
        binding.tvRecordingStatus.setTextColor(0xFF6C757D);
    }
}