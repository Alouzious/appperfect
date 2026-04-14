package com.example.pitchperfect.ui.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.pitchperfect.api.ApiClient;
import com.example.pitchperfect.data.MockDataRepository;
import com.example.pitchperfect.databinding.ActivityHomeBinding;
import com.example.pitchperfect.models.CsrfResponse;
import com.example.pitchperfect.models.DemoPitch;
import com.example.pitchperfect.models.PitchDeck;
import com.example.pitchperfect.models.PitchDeckListResponse;
import com.example.pitchperfect.ui.auth.LoginActivity;
import com.example.pitchperfect.ui.practice.PracticeActivity;
import com.example.pitchperfect.utils.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private SessionManager sessionManager;
    private DeckAdapter deckAdapter;
    private List<PitchDeck> deckList = new ArrayList<>();
    private String csrfToken = "";
    private boolean csrfFetchInProgress = false;

    private ActivityResultLauncher<String[]> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Pitch Perfect");
        }

        // Set welcome message
        binding.tvWelcome.setText("Welcome, " + sessionManager.getUsername() + "!");

        setupRecyclerView();
        setupFilePicker();
        setupClickListeners();
        fetchCsrfToken();
        loadDecks();
    }

    private void setupRecyclerView() {
        deckAdapter = new DeckAdapter(this, deckList);
        binding.rvDecks.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDecks.setAdapter(deckAdapter);
    }

    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) uploadDeck(uri);
                }
        );
    }

    private void setupClickListeners() {
        binding.btnUploadDeck.setOnClickListener(v ->
                filePickerLauncher.launch(new String[]{
                        "application/pdf",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "application/vnd.ms-powerpoint"
                })
        );

        // Add demo practice button if it exists
        if (binding.btnDemoPractice != null) {
            binding.btnDemoPractice.setOnClickListener(v -> showDemoPitchesDialog());
        }
    }

    private void showDemoPitchesDialog() {
        DemoPitch[] demoPitches = DemoPitch.getDemoPitches();
        String[] titles = new String[demoPitches.length];
        for (int i = 0; i < demoPitches.length; i++) {
            titles[i] = demoPitches[i].getTitle();
        }

        new AlertDialog.Builder(this)
                .setTitle("Choose a Demo Pitch")
                .setItems(titles, (dialog, which) -> {
                    DemoPitch selected = demoPitches[which];
                    startDemoPractice(selected);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startDemoPractice(DemoPitch demoPitch) {
        Intent intent = new Intent(this, PracticeActivity.class);
        intent.putExtra("deck_id", demoPitch.getId());
        intent.putExtra("deck_title", demoPitch.getTitle());
        intent.putExtra("is_demo", true);
        intent.putExtra("demo_description", demoPitch.getDescription());
        startActivity(intent);
    }

    private void fetchCsrfToken() {
        fetchCsrfToken(null);
    }

    private void fetchCsrfToken(Runnable onComplete) {
        if (csrfFetchInProgress) return;
        csrfFetchInProgress = true;
        ApiClient.getClient().getCsrfToken().enqueue(new Callback<CsrfResponse>() {
            @Override
            public void onResponse(Call<CsrfResponse> call, Response<CsrfResponse> response) {
                csrfFetchInProgress = false;
                if (response.isSuccessful() && response.body() != null) {
                    csrfToken = response.body().getCsrfToken();
                }
                storeCookiesFromResponse(response);
                if (onComplete != null) onComplete.run();
            }
            @Override
            public void onFailure(Call<CsrfResponse> call, Throwable t) {
                csrfFetchInProgress = false;
                if (onComplete != null) onComplete.run();
            }
        });
    }

    private void loadDecks() {
        binding.progressBar.setVisibility(View.VISIBLE);
        String cookie = sessionManager.getSessionCookie();

        ApiClient.getClient().getPitchDecks(cookie).enqueue(new Callback<PitchDeckListResponse>() {
            @Override
            public void onResponse(Call<PitchDeckListResponse> call, Response<PitchDeckListResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    deckList.clear();
                    deckList.addAll(response.body().getResults());
                    deckAdapter.notifyDataSetChanged();
                    binding.layoutEmpty.setVisibility(deckList.isEmpty() ? View.VISIBLE : View.GONE);
                } else if (response.code() == 403) {
                    logout();
                } else {
                    // Fallback to mock data
                    loadMockDecks();
                }
            }

            @Override
            public void onFailure(Call<PitchDeckListResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                // Fallback to mock data on network error
                loadMockDecks();
            }
        });
    }

    private void loadMockDecks() {
        deckList.clear();
        deckList.addAll(MockDataRepository.getMockPitchDecks());
        deckAdapter.notifyDataSetChanged();
        binding.layoutEmpty.setVisibility(View.GONE);
        
        // Show a subtle notification that demo data is loaded
        Toast.makeText(this, "Using demo data (offline mode)", Toast.LENGTH_SHORT).show();
    }

    private void uploadDeck(Uri uri) {
        if (csrfToken == null || csrfToken.isEmpty()) {
            Toast.makeText(this, "Getting security token...", Toast.LENGTH_SHORT).show();
            fetchCsrfToken(() -> {
                if (csrfToken != null && !csrfToken.isEmpty()) {
                    runOnUiThread(() -> uploadDeck(uri));
                } else {
                    runOnUiThread(() -> Toast.makeText(HomeActivity.this,
                            "Could not get CSRF token. Please try again.", Toast.LENGTH_SHORT).show());
                }
            });
            return;
        }

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            String fileName = getFileName(uri);

            // Copy to temp file
            File tempFile = new File(getCacheDir(), fileName);
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.close();
            inputStream.close();

            String mimeType = getContentResolver().getType(uri);
            if (mimeType == null || mimeType.isEmpty()) {
                mimeType = "application/octet-stream";
            }
            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), tempFile);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", fileName, requestFile);
            RequestBody titleBody = RequestBody.create(MediaType.parse("text/plain"), fileName.replace(".pptx", "").replace(".pdf", ""));

            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnUploadDeck.setEnabled(false);

            ApiClient.getClient().uploadPitchDeck(
                    csrfToken,
                    sessionManager.getSessionCookie(),
                    filePart,
                    titleBody
            ).enqueue(new Callback<PitchDeck>() {
                @Override
                public void onResponse(Call<PitchDeck> call, Response<PitchDeck> response) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnUploadDeck.setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(HomeActivity.this, "Deck uploaded! Processing...", Toast.LENGTH_SHORT).show();
                        loadDecks();
                    } else {
                        String backendError = readErrorBody(response);
                        String message = "Upload failed (" + response.code() + ")";
                        if (backendError != null && !backendError.isEmpty()) {
                            message = message + ": " + backendError;
                        }
                        Toast.makeText(HomeActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<PitchDeck> call, Throwable t) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnUploadDeck.setEnabled(true);
                    Toast.makeText(HomeActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
        }
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

    private String getFileName(Uri uri) {
        String result = "pitch_deck.pptx";
        try {
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) result = cursor.getString(idx);
                cursor.close();
            }
        } catch (Exception e) { /* use default */ }
        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(com.example.pitchperfect.R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == com.example.pitchperfect.R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDecks();
    }
}
