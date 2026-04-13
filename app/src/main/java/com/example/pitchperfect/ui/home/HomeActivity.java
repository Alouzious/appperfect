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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.pitchperfect.api.ApiClient;
import com.example.pitchperfect.databinding.ActivityHomeBinding;
import com.example.pitchperfect.models.CsrfResponse;
import com.example.pitchperfect.models.PitchDeck;
import com.example.pitchperfect.models.PitchDeckListResponse;
import com.example.pitchperfect.ui.auth.LoginActivity;
import com.example.pitchperfect.utils.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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

    private ActivityResultLauncher<String[]> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        setSupportActionBar(binding.toolbar);

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
    }

    private void fetchCsrfToken() {
        ApiClient.getClient(this).getCsrfToken().enqueue(new Callback<CsrfResponse>() {
            @Override
            public void onResponse(Call<CsrfResponse> call, Response<CsrfResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    csrfToken = response.body().getCsrfToken();
                }
            }
            @Override
            public void onFailure(Call<CsrfResponse> call, Throwable t) {}
        });
    }

    private void loadDecks() {
        binding.progressBar.setVisibility(View.VISIBLE);
        String cookie = sessionManager.getSessionCookie();

        ApiClient.getClient(this).getPitchDecks(cookie).enqueue(new Callback<PitchDeckListResponse>() {
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
                }
            }

            @Override
            public void onFailure(Call<PitchDeckListResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(HomeActivity.this, "Failed to load decks", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadDeck(Uri uri) {
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
            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), tempFile);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", fileName, requestFile);
            RequestBody titleBody = RequestBody.create(MediaType.parse("text/plain"), fileName.replace(".pptx", "").replace(".pdf", ""));

            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnUploadDeck.setEnabled(false);

            ApiClient.getClient(this).uploadPitchDeck(
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
                        Toast.makeText(HomeActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
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
