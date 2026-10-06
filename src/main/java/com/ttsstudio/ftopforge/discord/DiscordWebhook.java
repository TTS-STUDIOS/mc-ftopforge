package com.ttsstudio.ftopforge.discord;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTTP implementation of {@link HttpPoster} for Discord webhook delivery.
 * Smoke-tested only — exercised in production via real recalc cycles.
 * Bounded concurrent posts via semaphore to avoid runaway thread fan-out.
 */
public final class DiscordWebhook implements HttpPoster {
    private final Semaphore sem = new Semaphore(2);
    private final Logger log;

    public DiscordWebhook(Logger log) { this.log = log; }

    @Override
    public int post(String url, String body) {
        if (!sem.tryAcquire()) {
            if (log != null) log.warning("[discord] webhook semaphore full; skip");
            return -1;
        }
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("User-Agent", "FTopForge/1.2.0");
            c.setConnectTimeout(5000);
            c.setReadTimeout(10_000);
            c.setDoOutput(true);
            try (OutputStream os = c.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int code = c.getResponseCode();
            c.disconnect();
            return code;
        } catch (Exception e) {
            if (log != null) log.log(Level.WARNING, "[discord] webhook IO failed", e);
            return -1;
        } finally {
            sem.release();
        }
    }
}
