package com.ttsstudio.ftopforge.discord;

public interface HttpPoster {
    /** Returns HTTP status code, or -1 on transport error. */
    int post(String url, String jsonBody);
}
