package com.fizi.qrisvoice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;

Notification notification =
        new Notification.Builder(this, CHANNEL_ID)
    
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class TelegramForegroundService extends Service {

    private static final String CHANNEL_ID = "qris_voice";
    private static final String POLL_URL =
            "https://qris-voice.vercel.app/api/poll";

    private TextToSpeech tts;
    private volatile boolean running = true;
    private long offset = 0;
    private boolean initialized = false;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        Notification notification =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("QRIS Voice aktif")
                        .setContentText("Menunggu pembayaran...")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setOngoing(true)
                        .build();

        startForeground(1001, notification);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("id", "ID"));
                tts.setSpeechRate(0.95f);
            }
        });

        new Thread(() -> {

            while (running) {

                poll();

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                }
            }

        }).start();
    }

    private void poll() {

        try {

            URL url = new URL(
                    POLL_URL + "?offset=" + offset
            );

            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream()
                            )
                    );

            StringBuilder result = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            reader.close();
            connection.disconnect();

            JSONObject root =
                    new JSONObject(result.toString());

            if (!root.optBoolean("ok", false)) {
                return;
            }

            JSONArray updates =
                    root.optJSONArray("updates");

            if (updates == null) {
                return;
            }

            if (!initialized) {

                for (int i = 0; i < updates.length(); i++) {

                    JSONObject update =
                            updates.getJSONObject(i);

                    long updateId =
                            update.optLong("update_id", -1);

                    if (updateId >= 0) {
                        offset = updateId + 1;
                    }
                }

                initialized = true;
                return;
            }

            for (int i = 0; i < updates.length(); i++) {

                JSONObject update =
                        updates.getJSONObject(i);

                long updateId =
                        update.optLong("update_id", -1);

                String message =
                        update.optString("text", "").trim();

                if (updateId < 0) {
                    continue;
                }

                offset = updateId + 1;

                if (message.isEmpty()) {
                    continue;
                }

                if (message.equals("/start")) {
                    continue;
                }

                speak(message);
            }

        } catch (Exception ignored) {
        }
    }

    private void speak(String message) {

        if (tts == null) {
            return;
        }

        String spoken =
                "Pembayaran QRIS masuk. " + message;

        tts.speak(
                spoken,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "telegram_qris"
        );
    }

    private void createNotificationChannel() {

        NotificationManager manager =
                getSystemService(NotificationManager.class);

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "QRIS Voice",
                        NotificationManager.IMPORTANCE_LOW
                );

        manager.createNotificationChannel(channel);
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        running = false;

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    }
