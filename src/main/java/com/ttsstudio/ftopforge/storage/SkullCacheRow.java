package com.ttsstudio.ftopforge.storage;

import java.util.UUID;

public final class SkullCacheRow {
    public final UUID uuid;
    public final String textureValue;
    public final long fetchedAt;

    public SkullCacheRow(UUID uuid, String textureValue, long fetchedAt) {
        this.uuid = uuid;
        this.textureValue = textureValue;
        this.fetchedAt = fetchedAt;
    }
}
