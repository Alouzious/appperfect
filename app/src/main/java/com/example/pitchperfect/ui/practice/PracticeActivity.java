package com.example.pitchperfect.ui.practice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private static final String REFERER_URL = "https://pitch-perfect-api.onrender.com/";

    private MediaRecorder mediaRecorder;
    private File audioFile;
    private boolean isRecording = false;
    private int secondsElapsed = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
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

        binding.tvDeckTitle.setText(deckTitle != null ? deckTitle : "Practice Your Pitch");

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
        ApiClient.getClient(this).getCsrfToken().enqueue(new Callback<CsrfResponse>() {
            @Override
            public void onResponse(@NonNull Call<CsrfResponse> call,
                                   @NonNull Response<CsrfResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    csrfToken = response.body().getCsrfToken();
                }
            }
            @Override
            public void onFailure(@NonNull Call<CsrfResponse> call, @NonNull Throwable t) {}
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

        // First create a session
        PracticeSessionRequest request = new PracticeSessionRequest(
                deckId, pitchType, "pending", secondsElapsed, getTargetDuration(pitchType)
        );

        ApiClient.getClient(this).createPracticeSession(csrfToken, REFERER_URL, request).enqueue(new Callback<PracticeSession>() {
            @Override
            public void onResponse(@NonNull Call<PracticeSession> call, @NonNull Response<PracticeSession> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String sessionId = response.body().getId();
                    binding.tvProcessingStatus.setText("Sending audio to AI...");
                    submitAudioToSession(sessionId);
                } else {
                    showError("Failed to create session");
                }
            }
            @Override
            public void onFailure(@NonNull Call<PracticeSession> call, @NonNull Throwable t) {
                showError("Network error");
            }
        });
    }

    private void submitAudioToSession(String sessionId) {
        RequestBody requestFile = RequestBody.create(MediaType.parse("audio/m4a"), audioFile);
        MultipartBody.Part audioPart = MultipartBody.Part.createFormData("audio", "practice.m4a", requestFile);
        RequestBody duration = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(secondsElapsed));

        ApiClient.getClient(this).submitPracticeAudio(csrfToken, REFERER_URL, sessionId, audioPart, duration).enqueue(new Callback<PracticeSession>() {
            @Override
            public void onResponse(@NonNull Call<PracticeSession> call, @NonNull Response<PracticeSession> response) {
                if (response.isSuccessful()) {
                    binding.tvProcessingStatus.setText("Analyzing with AI... This may take 10-15 seconds");
                    pollForFeedback(sessionId, 0);
                } else {
                    showError("Failed to submit audio");
                }
            }
            @Override
            public void onFailure(@NonNull Call<PracticeSession> call, @NonNull Throwable t) {
                showError("Network error submitting audio");
            }
        });
    }

    private void pollForFeedback(String sessionId, int attempts) {
        if (attempts > 20) {
            showError("Analysis is taking long. Check back later.");
            return;
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> ApiClient.getClient(this).getPracticeFeedback(sessionId).enqueue(new Callback<PracticeFeedback>() {
            @Override
            public void onResponse(@NonNull Call<PracticeFeedback> call, @NonNull Response<PracticeFeedback> response) {
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
            public void onFailure(@NonNull Call<PracticeFeedback> call, @NonNull Throwable t) {
                pollForFeedback(sessionId, attempts + 1);
            }
        }), 3000); // poll every 3 seconds
    }

    private void loadPreviousSessions() {
        ApiClient.getClient(this).getPracticeSessions(deckId).enqueue(new Callback<PracticeListResponse>() {
            @Override
            public void onResponse(@NonNull Call<PracticeListResponse> call, @NonNull Response<PracticeListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sessionList.clear();
                    sessionList.addAll(response.body().getResults());
                    sessionAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(@NonNull Call<PracticeListResponse> call, @NonNull Throwable t) {}
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
    }
}
