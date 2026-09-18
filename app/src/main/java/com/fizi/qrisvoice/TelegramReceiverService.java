package com.fizi.qrisvoice;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class TelegramReceiverService extends NotificationListenerService {

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

        // Hanya Telegram
        if (!"org.telegram.messenger".equals(sbn.getPackageName())) {
            return;
        }

        Notification notification = sbn.getNotification();
        if (notification == null) return;

        Bundle extras = notification.extras;
        if (extras == null) return;

        String title = String.valueOf(
                extras.getCharSequence(Notification.EXTRA_TITLE, "")
        );

        String text = String.valueOf(
                extras.getCharSequence(Notification.EXTRA_TEXT, "")
        );

        String bigText = String.valueOf(
                extras.getCharSequence(Notification.EXTRA_BIG_TEXT, "")
        );

        String message = (title + " " + text + " " + bigText)
                .replaceAll("\\s+", " ")
                .trim();

        if (message.isEmpty()) return;

        String lower = message.toLowerCase(Locale.ROOT);

        // Hanya baca pesan dari QRIS Voice
        if (!lower.contains("qris voice")) return;

        long now = System.currentTimeMillis();

        if (message.equals(lastSpoken) &&
                now - lastTime < 15000) {
            return;
        }

        lastSpoken = message;
        lastTime = now;

        tts.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "telegram_qris"
        );
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
