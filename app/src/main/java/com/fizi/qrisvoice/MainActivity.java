package com.fizi.qrisvoice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextToSpeech tts;
    private volatile boolean running = true;
    private long offset = 0;
    private boolean initialized = false;

    private static final String POLL_URL =
            "https://qris-voice.vercel.app/api/poll";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(40, 50, 40, 40);
        box.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("QRIS VOICE");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        TextView info = new TextView(this);
        info.setText(
                "\nQRIS Voice aktif.\n\n" +
                "Menunggu pembayaran dari DANA..."
        );
        info.setTextSize(17);
        box.addView(info);

        Button test = new Button(this);
        test.setText("TES SUARA");
        test.setOnClickListener(v -> {
            if (tts != null) {
                tts.speak(
                        "Pembayaran QRIS diterima. Sepuluh ribu rupiah.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "test"
                );
            }
        });
        box.addView(test);

        setContentView(box);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("id", "ID"));
                tts.setSpeechRate(0.95f);
            }
        });

        startService(new Intent(this, TelegramForegroundService.class));
    }

    private void startPolling() {

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

            /*
             * Saat pertama kali polling,
             * lewati pesan Telegram lama.
             */
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

            /*
             * Pesan baru
             */
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

    @Override
    protected void onDestroy() {

        running = false;

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }
}
