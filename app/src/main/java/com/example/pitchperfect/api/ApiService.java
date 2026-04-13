package com.example.pitchperfect.api;

import com.example.pitchperfect.models.CsrfResponse;
import com.example.pitchperfect.models.LoginRequest;
import com.example.pitchperfect.models.LoginResponse;
import com.example.pitchperfect.models.PitchDeck;
import com.example.pitchperfect.models.PitchDeckListResponse;
import com.example.pitchperfect.models.PracticeFeedback;
import com.example.pitchperfect.models.PracticeListResponse;
import com.example.pitchperfect.models.PracticeSession;
import com.example.pitchperfect.models.PracticeSessionRequest;
import com.example.pitchperfect.models.RegisterRequest;
import com.example.pitchperfect.models.RegisterResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @GET("auth/csrf/")
    Call<CsrfResponse> getCsrfToken();

    @POST("auth/register/")
    Call<RegisterResponse> register(
            @Header("X-CSRFToken") String csrfToken,
            @Header("Cookie") String cookie,
            @Body RegisterRequest body
    );

    @POST("auth/login/")
    Call<LoginResponse> login(
            @Header("X-CSRFToken") String csrfToken,
            @Header("Cookie") String cookie,
            @Body LoginRequest body
    );

    @POST("auth/logout/")
    Call<Void> logout(
            @Header("X-CSRFToken") String csrfToken,
            @Header("Cookie") String cookie
    );

    @GET("pitches/")
    Call<PitchDeckListResponse> getPitchDecks(
            @Header("Cookie") String cookie
    );

    @Multipart
    @POST("pitches/upload/")
    Call<PitchDeck> uploadPitchDeck(
            @Header("X-CSRFToken") String csrfToken,
            @Header("Cookie") String cookie,
            @Part MultipartBody.Part file,
            @Part("title") RequestBody title
    );

    @DELETE("pitches/{deck_id}/delete/")
    Call<Void> deletePitchDeck(
            @Header("X-CSRFToken") String csrfToken,
            @Header("Cookie") String cookie,
            @Path("deck_id") String deckId
    );

    @POST("practice/sessions/")
    Call<PracticeSession> createPracticeSession(
            @Header("X-CSRFToken") String csrfToken,
            @Header("Cookie") String cookie,
            @Body PracticeSessionRequest body
    );

    @GET("practice/sessions/list/")
    Call<PracticeListResponse> getPracticeSessions(
            @Header("Cookie") String cookie,
            @Query("pitch_deck") String deckId
    );

    @GET("practice/sessions/{session_id}/feedback/")
    Call<PracticeFeedback> getPracticeFeedback(
            @Header("Cookie") String cookie,
            @Path("session_id") String sessionId
    );

    @Multipart
    @POST("practice/sessions/{session_id}/submit-audio/")
    Call<PracticeSession> submitPracticeAudio(
            @Header("X-CSRFToken") String csrfToken,
            @Header("Cookie") String cookie,
            @Path("session_id") String sessionId,
            @Part MultipartBody.Part audio,
            @Part("duration_seconds") RequestBody duration
    );
}
