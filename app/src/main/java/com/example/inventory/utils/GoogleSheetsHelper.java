package com.example.inventory.utils;

import android.util.Log;

import com.example.inventory.BuildConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoogleSheetsHelper {

    private static final String TAG = "GoogleSheetsHelper";
    private static final String WEBHOOK_URL = BuildConfig.WEBHOOK_URL;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public void submitLotteryCodesViaWebhook(List<String> codes, WebhookCallback callback) {
        executorService.execute(() -> {
            try {
                List<String> cleanedCodes = new ArrayList<>();
                for (String code : codes) {
                    String cleaned = LotteryTicketParser.cleanCode(code);
                    if (!cleaned.isEmpty()) cleanedCodes.add(cleaned);
                }

                if (cleanedCodes.isEmpty()) {
                    callback.onFailure("No valid codes to submit");
                    return;
                }

                Log.d(TAG, "Submitting " + cleanedCodes.size() + " codes via webhook");
                boolean success = sendWebhookRequest(buildPayload(cleanedCodes));
                if (success) {
                    callback.onSuccess(cleanedCodes.size());
                } else {
                    callback.onFailure("Webhook request failed — check logcat for details");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error submitting codes via webhook", e);
                callback.onFailure("Error: " + e.getMessage());
            }
        });
    }

    private String buildPayload(List<String> codes) {
        StringBuilder sb = new StringBuilder("{\"codes\":[");
        for (int i = 0; i < codes.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(codes.get(i)).append("\"");
        }
        return sb.append("]}").toString();
    }

    private boolean sendWebhookRequest(String payload) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(WEBHOOK_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes("UTF-8"));
            }

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "Webhook response code: " + responseCode);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    responseCode < 400 ? conn.getInputStream() : conn.getErrorStream(), "UTF-8"))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);
                Log.d(TAG, "Webhook response: " + response);
            }

            return responseCode >= 200 && responseCode < 300;

        } catch (ConnectException e) {
            Log.e(TAG, "Connection failed", e);
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Request timed out", e);
        } catch (IOException e) {
            Log.e(TAG, "IO error", e);
        }
        return false;
    }

    public interface WebhookCallback {
        void onSuccess(int codesSubmitted);
        void onFailure(String error);
    }
}
