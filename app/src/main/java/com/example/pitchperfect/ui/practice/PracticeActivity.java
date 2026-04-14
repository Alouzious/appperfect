package com.example.pitchperfect.ui.practice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.pitchperfect.R;
import com.example.pitchperfect.api.ApiClient;
import com.example.pitchperfect.databinding.ActivityPracticeBinding;
import com.example.pitchperfect.models.CsrfResponse;
import com.example.pitchperfect.models.PracticeListResponse;
import com.example.pitchperfect.models.PracticeSession;
import com.example.pitchperfect.ui.auth.LoginActivity;
import com.example.pitchperfect.ui.feedback.FeedbackActivity;
import com.example.pitchperfect.ui.home.HomeActivity;
import com.example.pitchperfect.utils.SessionManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PracticeActivity extends AppCompatActivity {

    private ActivityPracticeBinding binding;
    private SessionManager sessionManager;
    private SessionAdapter sessionAdapter;
    private final List<PracticeSession> sessionList = new ArrayList<>();

    private String deckId;
    private String deckTitle;
    private String csrfToken = "";

    private MediaRecorder mediaRecorder;
    private File audioFile;
    private boolean isRecording = false;
    private int secondsElapsed = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private final StringBuilder fullTranscript = new StringBuilder();
    private String currentPartialSpeech = "";
    private boolean isAIspeaking = false;
    private boolean isTtsReady = false;
    private boolean pendingIntro = false;
    
    private final Handler silenceHandler = new Handler(Looper.getMainLooper());
    private Runnable silenceRunnable;
    private static final long SILENCE_THRESHOLD = 1800; // Even snappier (1.8s) for ultra-responsiveness

    private static final int PERMISSION_REQUEST_CODE = 100;
    private final Random random = new Random();

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

        setupToolbar();
        setupSpinner();
        setupRecyclerView();
        setupClickListeners();
        setupSpeech();
        fetchCsrfToken();
        loadPreviousSessions();
    }

    private void setupSpeech() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    runOnUiThread(() -> binding.tvRecordingStatus.setText("I'm listening..."));
                }
                @Override
                public void onBeginningOfSpeech() {
                    stopSilenceTimer();
                }
                @Override
                public void onRmsChanged(float rmsdB) {}
                @Override
                public void onBufferReceived(byte[] buffer) {}
                @Override
                public void onEndOfSpeech() {}
                @Override
                public void onError(int error) {
                    if (isRecording && !isAIspeaking) {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> startListening(), 400);
                    }
                }
                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        fullTranscript.append(text).append(". ");
                        currentPartialSpeech = "";
                        runOnUiThread(() -> {
                            binding.tvTranscript.setText(fullTranscript.toString());
                            resetSilenceTimer(text);
                        });
                    }
                    if (isRecording && !isAIspeaking) startListening();
                }
                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        currentPartialSpeech = matches.get(0);
                        runOnUiThread(() -> {
                            binding.tvTranscript.setText(fullTranscript.toString() + currentPartialSpeech);
                            stopSilenceTimer();
                        });
                    }
                }
                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        }

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
                isTtsReady = true;
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        isAIspeaking = true;
                        runOnUiThread(() -> {
                            binding.tvRecordingStatus.setText("AI is speaking...");
                            if (speechRecognizer != null) speechRecognizer.stopListening();
                        });
                    }
                    @Override
                    public void onDone(String utteranceId) {
                        isAIspeaking = false;
                        if (isRecording) {
                            runOnUiThread(() -> {
                                if (utteranceId.equals("intro")) {
                                    startActualRecording();
                                } else {
                                    startListening();
                                }
                            });
                        }
                    }
                    @Override
                    public void onError(String utteranceId) {
                        isAIspeaking = false;
                        if (isRecording) runOnUiThread(() -> startListening());
                    }
                });
                if (pendingIntro) {
                    pendingIntro = false;
                    runOnUiThread(this::startIntroFlow);
                }
            }
        });
    }

    private void resetSilenceTimer(String lastChunk) {
        stopSilenceTimer();
        silenceRunnable = () -> handleConversationalAI(lastChunk.toLowerCase());
        silenceHandler.postDelayed(silenceRunnable, SILENCE_THRESHOLD);
    }

    private void stopSilenceTimer() {
        if (silenceRunnable != null) {
            silenceHandler.removeCallbacks(silenceRunnable);
        }
    }

    private void handleConversationalAI(String text) {
        if (!isRecording || isAIspeaking) return;
        
        String reply;
        text = text.trim();
        
        if (text.isEmpty()) return;

        // Intelligent keyword detection for instant conversation
        if (text.contains("hello") || text.contains("hi") || text.contains("hey")) {
            reply = "Hey! I'm all ears. What's the big problem your pitch is solving?";
        } else if (text.contains("problem") || text.contains("pain") || text.contains("need")) {
            reply = "That sounds like a serious issue. How does your solution make life better for the users?";
        } else if (text.contains("solution") || text.contains("product") || text.contains("app") || text.contains("idea")) {
            reply = "I see! And what makes your idea unique compared to existing products?";
        } else if (text.contains("team") || text.contains("founder") || text.contains("we are") || text.contains("experience")) {
            reply = "A strong team is the foundation. What's your team's secret sauce?";
        } else if (text.contains("money") || text.contains("revenue") || text.contains("business") || text.contains("cost")) {
            reply = "The business model is key. How do you plan to scale and stay profitable?";
        } else if (text.contains("market") || text.contains("user") || text.contains("customer") || text.contains("size")) {
            reply = "Interesting! How did you validate that there's a real demand in this market?";
        } else if (text.contains("growth") || text.contains("scale") || text.contains("next") || text.contains("future")) {
            reply = "Ambitious! What's the single biggest obstacle to reaching your next milestone?";
        } else if (text.contains("competitor") || text.contains("rival") || text.contains("better than")) {
            reply = "Competition is tough! What's your long-term defensive strategy against them?";
        } else if (text.length() > 3) {
            // Contextual fillers if no keywords are found but the user said SOMETHING
            String[] deepReplies = {
                "I like the direction you're taking. Can you dive deeper into that?",
                "That's a solid point. How does it impact your overall strategy?",
                "Interesting insight. Tell me more about the logic behind it.",
                "I'm following you. What happens after that part of the plan?",
                "That makes sense. How do you plan to execute that specific part?",
                "Got it. And what's the feedback from people you've shown this to?"
            };
            reply = deepReplies[random.nextInt(deepReplies.length)];
        } else {
            reply = "Interesting. Keep going, I'm listening!";
        }
        
        speakReply(reply, "reply_" + System.currentTimeMillis());
    }

    private void startListening() {
        if (speechRecognizer == null || isAIspeaking || !isRecording) return;
        runOnUiThread(() -> {
            try {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                speechRecognizer.startListening(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void speakReply(String text, String id) {
        if (textToSpeech != null && isTtsReady) {
            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id);
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, id);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(deckTitle != null ? deckTitle : "Practice");
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
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
                if (!isRecording) startIntroFlow();
            }
        });
        
        binding.btnStop.setOnClickListener(v -> stopRecordingAndSubmit());
        
        binding.btnMockData.setOnClickListener(v -> {
            Intent intent = new Intent(this, FeedbackActivity.class);
            intent.putExtra("session_id", "mock_session");
            intent.putExtra("deck_id", deckId);
            startActivity(intent);
        });
    }

    private void startIntroFlow() {
        if (!isTtsReady) {
            pendingIntro = true;
            binding.tvRecordingStatus.setText("AI is warming up...");
            return;
        }
        binding.btnRecord.setEnabled(false);
        binding.tvRecordingStatus.setText("Talking to AI...");
        isRecording = true;
        speakReply("Hello! This is Pitch Perfect. I'm ready to listen to your amazing pitch. Whenever you're ready, start speaking!", "intro");
    }

    private void startActualRecording() {
        try {
            audioFile = new File(getCacheDir(), "practice_audio.m4a");
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();

            secondsElapsed = 0;
            fullTranscript.setLength(0);
            currentPartialSpeech = "";
            runOnUiThread(() -> {
                binding.tvTranscript.setText("");
                binding.btnStop.setEnabled(true);
                binding.tvRecordingStatus.setText("Recording...");
                binding.tvRecordingStatus.setTextColor(0xFFDC3545);
            });
            startTimer();
            startListening();
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
                binding.btnRecord.setEnabled(true);
            });
            isRecording = false;
        }
    }

    private void stopRecordingAndSubmit() {
        if (!isRecording) return;
        stopSilenceTimer();
        
        try {
            runOnUiThread(() -> {
                binding.tvRecordingStatus.setText("AI is analyzing...");
                binding.layoutProcessing.setVisibility(View.VISIBLE);
                binding.tvProcessingStatus.setText("Deep Analysis in progress...");
            });

            // Combine everything the user said
            String transcript = (fullTranscript.toString() + currentPartialSpeech).toLowerCase().trim();
            String feedback;
            
            // Intelligence fix: If the transcript has even a little bit of text, NEVER say "I didn't hear you"
            if (transcript.isEmpty() || transcript.length() < 3) {
                feedback = "I'm eager to hear your idea! Why don't you start by telling me about the problem you're solving?";
            } else {
                // Topic-based analysis for final feedback
                if (transcript.contains("team") || transcript.contains("founder") || transcript.contains("we")) {
                    feedback = "I noticed you've got a solid team behind this. What's the biggest challenge you face in keeping everyone aligned on the vision?";
                } else if (transcript.contains("market") || transcript.contains("customer") || transcript.contains("user")) {
                    feedback = "Your market focus is impressive. If a major competitor entered this space tomorrow, how would you defend your territory?";
                } else if (transcript.contains("money") || transcript.contains("revenue") || transcript.contains("profit")) {
                    feedback = "I hear a clear business model. How quickly do you think you can reach your first major revenue milestone?";
                } else if (transcript.length() > 50) {
                    feedback = "That was a very detailed session. You covered a lot of ground. If you had to boil your entire pitch down to one 'killer feature', what would it be?";
                } else {
                    feedback = "I followed your points so far. Can you dive a bit deeper into the actual technology or process that makes your product work?";
                }
            }

            speakReply(feedback, "analysis_reply");
            
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                runOnUiThread(() -> {
                    binding.layoutProcessing.setVisibility(View.GONE);
                    // Automatically resume listening so the user can answer the AI's question!
                    startListening();
                });
            }, 3000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchCsrfToken() {
        ApiClient.getClient(this).getCsrfToken().enqueue(new Callback<CsrfResponse>() {
            @Override
            public void onResponse(@NonNull Call<CsrfResponse> call, @NonNull Response<CsrfResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    csrfToken = response.body().getCsrfToken();
                }
            }
            @Override
            public void onFailure(@NonNull Call<CsrfResponse> call, @NonNull Throwable t) {}
        });
    }

    private void loadPreviousSessions() {
        if (deckId == null) return;
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
                runOnUiThread(() -> binding.tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startIntroFlow();
        } else {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        stopSilenceTimer();
        if (mediaRecorder != null) { try { mediaRecorder.stop(); } catch(Exception e){} mediaRecorder.release(); mediaRecorder = null; }
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (textToSpeech != null) { textToSpeech.stop(); textToSpeech.shutdown(); }
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
}
