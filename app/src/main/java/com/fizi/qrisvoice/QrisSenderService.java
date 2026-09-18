package com.fizi.qrisvoice;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class QrisSenderService extends NotificationListenerService {

    private static final String VERCEL_URL =
            "https://qris-voice.vercel.app/api/send";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        if (sbn == null) return;

        // Hanya baca notifikasi DANA
        if (!"id.dana".equals(sbn.getPackageName())) return;

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

        // Hanya kirim notifikasi yang berhubungan dengan pembayaran
        String lower = message.toLowerCase();

        boolean payment =
                lower.contains("pembayaran") ||
                lower.contains("diterima") ||
                lower.contains("qris") ||
                lower.contains("masuk") ||
                lower.contains("receive") ||
                lower.contains("received");

        if (!payment) return;

        // Jangan kirim transaksi pembayaran keluar
        if (lower.contains("kamu membayar") ||
                lower.contains("pembayaran berhasil ke")) {
            return;
        }

        sendToVercel(message);
    }

    private void sendToVercel(String message) {

        new Thread(() -> {

            try {

                URL url = new URL(VERCEL_URL);

                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );

                connection.setDoOutput(true);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                String json =
                        "{\"message\":\"" +
                        escapeJson(message) +
                        "\"}";

                OutputStream output =
                        connection.getOutputStream();

                output.write(json.getBytes("UTF-8"));
                output.flush();
                output.close();

                connection.getResponseCode();

                connection.disconnect();

            } catch (Exception ignored) {
            }

        }).start();
    }

    private String escapeJson(String text) {

        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
