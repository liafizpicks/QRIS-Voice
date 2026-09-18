package com.fizi.qrisvoice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {
    private TextToSpeech tts;

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
        info.setText("\nBaca notifikasi pembayaran DANA dengan suara.\n\n1. Aktifkan akses Notification Listener.\n2. Pastikan notifikasi DANA aktif.\n3. Saat pembayaran masuk, QRIS Voice akan membacakan notifikasi.");
        info.setTextSize(17);
        box.addView(info);

        Button settings = new Button(this);
        settings.setText("AKTIFKAN AKSES NOTIFIKASI");
        settings.setOnClickListener(v ->
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));
        box.addView(settings);

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
    }

    @Override
    protected void onDestroy() {
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }
}
