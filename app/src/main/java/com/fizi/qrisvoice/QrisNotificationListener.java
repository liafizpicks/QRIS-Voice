package com.fizi.qrisvoice;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class QrisNotificationListener extends NotificationListenerService {
    private static final String TAG = "QRIS_VOICE";
    private TextToSpeech tts;
    private String lastSpoken = "";
    private long lastTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("id", "ID"));
                tts.setSpeechRate(0.95f);
            }
        });
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || tts == null) return;

        if (!"id.dana".equals(sbn.getPackageName())) return;

        Notification n = sbn.getNotification();
        Bundle e = n.extras;
        if (e == null) return;

        String title = String.valueOf(
                e.getCharSequence(Notification.EXTRA_TITLE, "")
        );
        String text = String.valueOf(
                e.getCharSequence(Notification.EXTRA_TEXT, "")
        );
        String big = String.valueOf(
                e.getCharSequence(Notification.EXTRA_BIG_TEXT, "")
        );

        String raw = (title + " " + text + " " + big).trim();
        if (raw.isEmpty()) return;

        String lower = raw.toLowerCase(Locale.ROOT);

        boolean payment =
                lower.contains("pembayaran") ||
                lower.contains("diterima") ||
                lower.contains("qris") ||
                lower.contains("masuk") ||
                lower.contains("receive") ||
                lower.contains("received");

        if (!payment) return;

        if (lower.contains("kamu membayar") ||
                lower.contains("pembayaran berhasil ke")) return;

        long now = System.currentTimeMillis();

        if (raw.equals(lastSpoken) && now - lastTime < 15000) return;

        lastSpoken = raw;
        lastTime = now;

        String spoken = makeSpeech(raw);

        Log.d(TAG, "Incoming: " + raw);
        Log.d(TAG, "Speech: " + spoken);

        tts.speak(
                spoken,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "qris_payment"
        );
    }

    private String makeSpeech(String raw) {
        String s = raw.replaceAll("\\s+", " ").trim();

        s = s.replaceAll(
                "(?i)Rp\\.?\\s*([0-9][0-9.,]*)",
                "rupiah $1"
        );

        s = s.replaceAll(
                "(?i)IDR\\s*([0-9][0-9.,]*)",
                "rupiah $1"
        );

        return "Pembayaran QRIS masuk. " + s;
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }
}
