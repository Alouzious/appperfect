package com.example.pitchperfect.utils;

import android.content.Context;
import android.content.Intent;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.SpeechRecognizer.RecognizerIntent;

public class SpeechRecognitionHelper {

    private SpeechRecognizer speechRecognizer;
    private RecognitionListener listener;
    private Context context;
    private boolean isListening = false;

    public interface SpeechRecognitionListener {
        void onResults(String text);
        void onError(String error);
        void onReadyForSpeech();
        void onEndOfSpeech();
    }

    private SpeechRecognitionListener speechListener;

    public SpeechRecognitionHelper(Context context) {
        this.context = context;
        initializeSpeechRecognizer();
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            setupListener();
        }
    }

    private void setupListener() {
        listener = new RecognitionListener() {
            @Override
            public void onReadyForSpeech(android.os.Bundle params) {
                if (speechListener != null) speechListener.onReadyForSpeech();
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                if (speechListener != null) speechListener.onEndOfSpeech();
            }

            @Override
            public void onError(int error) {
                String errorMessage = getErrorMessage(error);
                if (speechListener != null) speechListener.onError(errorMessage);
            }

            @Override
            public void onResults(android.os.Bundle results) {
                java.util.ArrayList<String> matches = 
                    results.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    if (speechListener != null) speechListener.onResults(matches.get(0));
                }
            }

            @Override
            public void onPartialResults(android.os.Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, android.os.Bundle params) {}
        };
    }

    public void startListening(SpeechRecognitionListener listener) {
        this.speechListener = listener;
        if (speechRecognizer != null && !isListening) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                           RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            
            speechRecognizer.setRecognitionListener(listener);
            speechRecognizer.startListening(intent);
            isListening = true;
        }
    }

    public void stopListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
        }
    }

    public void release() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    public boolean isListening() {
        return isListening;
    }

    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech input detected";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognizer is busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "Speech input timeout";
            default:
                return "Unknown error";
        }
    }
}
