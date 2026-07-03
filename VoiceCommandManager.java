package com.jarvis.automation;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telecom.TelecomManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

/**
 * Wraps Android's on-device SpeechRecognizer to provide continuous
 * hands-free listening while a trusted headset is connected. Maps
 * recognized phrases to VIP calling or media playback.
 *
 * Note: this uses the standard SpeechRecognizer API (same one backing
 * "Ok Google" style flows), not any private/undocumented Assistant hook —
 * Google does not expose a public API to inject commands into its own
 * Assistant app.
 */
public class VoiceCommandManager {

    private static final String TAG = "VoiceCommandManager";

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private SpeechRecognizer speechRecognizer;
    private boolean listening = false;

    public VoiceCommandManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void startListening() {
        if (listening) return;
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition not available on this device");
            return;
        }
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECORD_AUDIO not granted, cannot start voice control");
            return;
        }

        listening = true;
        mainHandler.post(this::beginRecognitionCycle);
    }

    public void stopListening() {
        listening = false;
        mainHandler.post(() -> {
            if (speechRecognizer != null) {
                speechRecognizer.stopListening();
                speechRecognizer.destroy();
                speechRecognizer = null;
            }
        });
    }

    private void beginRecognitionCycle() {
        if (!listening) return;

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { }
            @Override public void onBeginningOfSpeech() { }
            @Override public void onRmsChanged(float rmsdB) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { }

            @Override
            public void onError(int error) {
                // Restart the listening cycle (common with continuous on-device recognition).
                restartCycleSoon();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    handlePhrase(matches.get(0).toLowerCase(Locale.getDefault()));
                }
                restartCycleSoon();
            }

            @Override public void onPartialResults(Bundle partialResults) { }
            @Override public void onEvent(int eventType, Bundle params) { }
        });

        speechRecognizer.startListening(recognizerIntent);
    }

    private void restartCycleSoon() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (listening) {
            mainHandler.postDelayed(this::beginRecognitionCycle, 500);
        }
    }

    private void handlePhrase(String phrase) {
        Log.d(TAG, "Heard: " + phrase);

        for (Map.Entry<String, String> entry : VipConfig.VIP_CONTACTS.entrySet()) {
            if (phrase.contains(entry.getKey())) {
                placeCall(entry.getValue());
                return;
            }
        }

        for (Map.Entry<String, String> entry : VipConfig.PLAYLISTS.entrySet()) {
            if (phrase.contains(entry.getKey())) {
                launchPlaylist(entry.getValue());
                return;
            }
        }
    }

    private void placeCall(String phoneNumber) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "CALL_PHONE not granted");
            return;
        }
        Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(callIntent);
    }

    private void launchPlaylist(String spotifyUri) {
        Intent playIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUri));
        playIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (playIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(playIntent);
        } else {
            Log.w(TAG, "Spotify not installed, cannot handle URI: " + spotifyUri);
        }
    }
}
