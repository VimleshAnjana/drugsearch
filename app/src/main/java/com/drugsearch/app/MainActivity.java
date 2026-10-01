package com.drugsearch.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final int REQ_MIC = 101;

    private WebView web;
    private SpeechRecognizer recognizer;
    private String pendingLang = "en-IN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);

        web.setWebViewClient(new WebViewClient());
        // Android WebView has no built-in speech recognition, so the page
        // calls this bridge instead.
        web.addJavascriptInterface(new Bridge(), "AndroidSpeech");
        web.loadUrl("file:///android_asset/index.html");
    }

    private void js(String code) {
        web.evaluateJavascript(code, null);
    }

    private void state(String st) {
        js("window.onNativeState && window.onNativeState(" + JSONObject.quote(st) + ")");
    }

    private void text(String t) {
        js("window.onNativeSpeech && window.onNativeSpeech(" + JSONObject.quote(t) + ")");
    }

    private void beginListening(String lang) {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            state("unavailable");
            return;
        }
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { state("start"); }
            @Override public void onBeginningOfSpeech() { }
            @Override public void onRmsChanged(float rmsdB) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { }
            @Override public void onError(int error) { state("error:" + error); }
            @Override public void onEvent(int eventType, Bundle params) { }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> l = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (l != null && !l.isEmpty()) text(l.get(0));
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> l = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (l != null && !l.isEmpty()) text(l.get(0));
                state("end");
            }
        });

        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizer.startListening(i);
    }

    private class Bridge {
        @JavascriptInterface
        public void start(final String lang) {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    pendingLang = lang;
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
                    } else {
                        beginListening(lang);
                    }
                }
            });
        }

        @JavascriptInterface
        public void stop() {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    if (recognizer != null) recognizer.stopListening();
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MIC) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                beginListening(pendingLang);
            } else {
                state("denied");
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) recognizer.destroy();
        super.onDestroy();
    }
}
