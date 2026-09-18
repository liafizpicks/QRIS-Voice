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

        String packageName = sbn.getPackageName();

        // =====================================================
        // TELEGRAM
        // =====================================================
        if ("org.telegram.messenger".equals(packageName)) {

            readTelegramNotification(sbn);

            return;
        }

        // =====================================================
        // DANA
        // =====================================================
        if ("id.dana".equals(packageName)) {

            readDanaNotification(sbn);

            return;
        }
    }

    // =========================================================
    // BACA TELEGRAM
    // =========================================================

    private void readTelegramNotification(StatusBarNotification sbn) {

        Notification n = sbn.getNotification();

        if (n == null || n.extras == null) return;

        Bundle e = n.extras;

        String title = String.valueOf(
                e.getCharSequence(Notification.EXTRA_TITLE, "")
        );

        String text = String.valueOf(
                e.getCharSequence(Notification.EXTRA_TEXT, "")
        );

        String big = String.valueOf(
                e.getCharSequence(Notification.EXTRA_BIG_TEXT, "")
        );

        String message = (title + " " + text + " " + big)
                .replaceAll("\\s+", " ")
                .trim();

        if (message.isEmpty()) return;

        String lower = message.toLowerCase(Locale.ROOT);

        // Hanya baca pesan yang ditandai QRIS VOICE
        if (!lower.contains("qris voice")) {
            return;
        }

        speakMessage(
                message,
                "telegram_qris"
        );
    }

    // =========================================================
    // BACA DANA
    // =========================================================

    private void readDanaNotification(StatusBarNotification sbn) {

        Notification n = sbn.getNotification();

        if (n == null || n.extras == null) return;

        Bundle e = n.extras;

        String title = String.valueOf(
                e.getCharSequence(Notification.EXTRA_TITLE, "")
        );

        String text = String.valueOf(
                e.getCharSequence(Notification.EXTRA_TEXT, "")
        );

        String big = String.valueOf(
                e.getCharSequence(Notification.EXTRA_BIG_TEXT, "")
        );

        String raw = (title + " " + text + " " + big)
                .replaceAll("\\s+", " ")
                .trim();

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

        // Jangan baca transaksi keluar
        if (lower.contains("kamu membayar") ||
                lower.contains("pembayaran berhasil ke")) {
            return;
        }

        String spoken = makeDanaSpeech(raw);

        speakMessage(
                spoken,
                "qris_payment"
        );
    }

    // =========================================================
    // TEXT TO SPEECH
    // =========================================================

    private void speakMessage(String message, String utteranceId) {

        long now = System.currentTimeMillis();

        if (message.equals(lastSpoken) &&
                now - lastTime < 15000) {
            return;
        }

        lastSpoken = message;
        lastTime = now;

        Log.d(TAG, "Incoming: " + message);

        tts.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                utteranceId
        );
    }

    // =========================================================
    // FORMAT DANA
    // =========================================================

    private String makeDanaSpeech(String raw) {

        String s = raw
                .replaceAll("\\s+", " ")
                .trim();

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

    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    public void onDestroy() {

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }
}
