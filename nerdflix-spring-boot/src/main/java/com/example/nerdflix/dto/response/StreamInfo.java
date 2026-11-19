package com.example.nerdflix.dto.response;

import java.time.Instant;

public record StreamInfo(String manifestUrl, long expEpochSeconds) {


    public long expInSecondsFromNow() {
        return Math.max(0, expEpochSeconds - Instant.now().getEpochSecond());
    }
}

