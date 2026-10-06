package com.ttsstudio.ftopforge.core;

import com.ttsstudio.ftopforge.hooks.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class BalanceAggregator {

    private final VaultHook vault;
    private final boolean includeBalance;
    private final boolean includeOffline;

    public BalanceAggregator(VaultHook vault, boolean includeBalance, boolean includeOffline) {
        this.vault = vault;
        this.includeBalance = includeBalance;
        this.includeOffline = includeOffline;
    }

    public void aggregate(UUID memberUuid, FactionSnapshot.Builder snapshot) {
        if (!includeBalance) return;
        if (!includeOffline) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(memberUuid);
            if (!op.isOnline()) return;
        }
        long bal = vault.balanceOf(memberUuid);
        snapshot.addBalanceValue(bal);
        snapshot.maybeRichest(memberUuid.toString(), bal);
    }
}
