package com.ttsstudio.ftopforge.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public final class FtopForgePlaceholders extends PlaceholderExpansion {
    private final PlaceholderResolver resolver;
    private final String pluginVersion;

    public FtopForgePlaceholders(PlaceholderResolver resolver, String pluginVersion) {
        this.resolver = resolver;
        this.pluginVersion = pluginVersion;
    }

    @Override public String getIdentifier() { return "ftopforge"; }
    @Override public String getAuthor() { return "TTS-Studio"; }
    @Override public String getVersion() { return pluginVersion; }
    @Override public boolean persist() { return false; }

    @Override public String onRequest(OfflinePlayer player, String params) {
        UUID uuid = player != null ? player.getUniqueId() : null;
        return resolver.resolve(uuid, params);
    }
}
