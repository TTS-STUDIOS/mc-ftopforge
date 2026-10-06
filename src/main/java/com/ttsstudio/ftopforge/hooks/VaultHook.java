package com.ttsstudio.ftopforge.hooks;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

public class VaultHook {

    private final Economy economy;

    private VaultHook(Economy economy) {
        this.economy = economy;
    }

    public static VaultHook initOrFail() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            throw new IllegalStateException("Vault is not enabled. FTopForge requires Vault + an economy provider.");
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null || rsp.getProvider() == null) {
            throw new IllegalStateException("No Vault economy provider registered (need Essentials/CMI/etc.).");
        }
        return new VaultHook(rsp.getProvider());
    }

    public long balanceOf(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        try {
            double bal = economy.getBalance(op);
            if (Double.isNaN(bal) || Double.isInfinite(bal) || bal < 0) return 0L;
            return (long) bal;
        } catch (Throwable t) {
            return 0L;
        }
    }
}
