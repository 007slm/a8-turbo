package com.ojp.patch.reporter;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OJP Event Reporter
 * 提供零依赖的、带重试与指数退避机制的轻量级 Webhook 事件上报功能。
 */
public class OjpEventReporter {
    private static final Logger LOG = LoggerFactory.getLogger(OjpEventReporter.class);
    private static final String WEBHOOK_URL = "http://ojp-server:8010/api/cache/events";
    private static final int TIMEOUT_MS = 3000; // 3秒超时
    private static final int MAX_RETRIES = 3;

    public static void reportEventAsync(String eventType, String detailKey, String detailVal) {
        String payload = String.format(
            "{\"event\":\"%s\",\"%s\":\"%s\",\"timestamp\":%d}",
            eventType, detailKey, detailVal, System.currentTimeMillis()
        );
        
        CompletableFuture.runAsync(() -> {
            long retryDelayMs = 500;
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(WEBHOOK_URL);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; utf-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(TIMEOUT_MS);
                    conn.setReadTimeout(TIMEOUT_MS);

                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }

                    int code = conn.getResponseCode();
                    if (code >= 200 && code < 300) {
                        LOG.info("OJP Event [{}] reported successfully.", eventType);
                        return; // 成功，退出
                    }
                    LOG.warn("OJP Event [{}] post failed (Attempt {}/{}) with response code: {}", 
                        eventType, attempt, MAX_RETRIES, code);
                } catch (Exception e) {
                    LOG.warn("OJP Event [{}] post error (Attempt {}/{}): {}", 
                        eventType, attempt, MAX_RETRIES, e.getMessage());
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
                
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= 2; // 指数退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            LOG.error("OJP Event [{}] failed to report after {} attempts.", eventType, MAX_RETRIES);
        });
    }
}
