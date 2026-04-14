package com.example.pitchperfect.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class TextToSpeechHelper {

    private TextToSpeech tts;
    private Context context;
    private boolean isReady = false;

    public TextToSpeechHelper(Context context) {
        this.context = context;
        initializeTTS();
    }

    private void initializeTTS() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                isReady = true;
            }
        });
    }

    public void speak(String text) {
        if (isReady && tts != null && text != null && !text.isEmpty()) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null);
        }
    }

    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    public void release() {
        if (tts != null) {
            tts.shutdown();
        }
    }

    public boolean isReady() {
        return isReady;
    }
}
