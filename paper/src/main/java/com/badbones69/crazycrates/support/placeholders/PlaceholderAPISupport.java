package com.badbones69.crazycrates.support.placeholders;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.CrazyHandler;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.tasks.BukkitUserManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.Locale;

public class PlaceholderAPISupport extends PlaceholderExpansion {

    @NotNull
    private final CrazyCrates plugin = CrazyCrates.get();

    @NotNull
    private final CrazyHandler crazyHandler = this.plugin.getCrazyHandler();

    @NotNull
    private final BukkitUserManager userManager = this.crazyHandler.getUserManager();

    private final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.of("pt", "BR"));

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (player instanceof Player human) {
            for (Crate crate : crazyHandler.getCrateManager().getUsableCrates()) {
                if (identifier.equalsIgnoreCase(crate.getName())) {
                    return numberFormat.format(userManager.getVirtualKeys(human.getUniqueId(), crate.getName()));
                } else {
                    return "0";
                }
            }
        }
        return "N/A";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return this.plugin.getName().toLowerCase();
    }

    @Override
    @NotNull
    public String getAuthor() {
        return this.plugin.getDescription().getAuthors().toString();
    }

    @Override
    @NotNull
    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }
}
