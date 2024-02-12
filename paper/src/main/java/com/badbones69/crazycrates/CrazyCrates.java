package com.badbones69.crazycrates;

import com.badbones69.crazycrates.api.MigrateManager;
import com.badbones69.crazycrates.api.enums.Permissions;
import com.badbones69.crazycrates.listeners.BrokeLocationsListener;
import com.badbones69.crazycrates.listeners.CrateControlListener;
import com.badbones69.crazycrates.listeners.MiscListener;
import com.badbones69.crazycrates.listeners.crates.*;
import com.badbones69.crazycrates.listeners.menus.CrateAdminListener;
import com.badbones69.crazycrates.listeners.menus.CrateMenuListener;
import com.badbones69.crazycrates.listeners.menus.CratePreviewListener;
import com.badbones69.crazycrates.listeners.menus.CrateTierListener;
import com.badbones69.crazycrates.tasks.BukkitUserManager;
import com.badbones69.crazycrates.tasks.crates.CrateManager;
import com.badbones69.crazycrates.tasks.crates.other.quadcrates.SessionManager;
import com.badbones69.crazycrates.api.utils.MsgUtils;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import com.badbones69.crazycrates.common.config.types.ConfigKeys;
import com.badbones69.crazycrates.api.FileManager;
import dev.triumphteam.cmd.bukkit.BukkitCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import com.badbones69.crazycrates.common.config.ConfigManager;
import com.badbones69.crazycrates.support.placeholders.PlaceholderAPISupport;
import com.badbones69.crazycrates.support.libraries.PluginSupport;
import java.util.Arrays;
import java.util.Timer;
import java.util.function.Supplier;
import java.util.logging.Level;

public class CrazyCrates extends JavaPlugin {

    @NotNull
    public static CrazyCrates get() {
        return JavaPlugin.getPlugin(CrazyCrates.class);
    }

    @NotNull
    private final BukkitCommandManager<CommandSender> commandManager = BukkitCommandManager.create(this);

    private CrazyHandler crazyHandler;
    private Timer timer;

    @Override
    public void onEnable() {
        // Migrate configurations.
        MigrateManager.migrate();

        this.timer = new Timer();

        registerPermissions();

        // Load version 2 of crazycrates
        this.crazyHandler = new CrazyHandler(this);
        this.crazyHandler.load();

        // Clean if we have to.
        this.crazyHandler.cleanFiles();

        // Register listeners
        this.crazyHandler.getModuleLoader().addModule(new CrateTierListener());
        this.crazyHandler.getModuleLoader().addModule(new CratePreviewListener());
        this.crazyHandler.getModuleLoader().addModule(new CrateAdminListener());
        this.crazyHandler.getModuleLoader().addModule(new CrateMenuListener());

        this.crazyHandler.getModuleLoader().load();

        PluginManager pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(new BrokeLocationsListener(), this);

        pluginManager.registerEvents(new CrateControlListener(), this);
        pluginManager.registerEvents(new MobileCrateListener(), this);
        pluginManager.registerEvents(new CosmicCrateListener(), this);
        pluginManager.registerEvents(new QuadCrateListener(), this);
        pluginManager.registerEvents(new CrateOpenListener(), this);
        pluginManager.registerEvents(new WarCrateListener(), this);
        pluginManager.registerEvents(new MiscListener(), this);

        if (isLogging()) {
            String prefix = this.crazyHandler.getConfigManager().getConfig().getProperty(ConfigKeys.console_prefix);

            // Print dependency garbage
            for (PluginSupport value : PluginSupport.values()) {
                if (value.isPluginEnabled()) {
                    getServer().getConsoleSender().sendMessage(MsgUtils.color(prefix + "&6&l" + value.name() + " &a&lFOUND"));
                } else {
                    getServer().getConsoleSender().sendMessage(MsgUtils.color(prefix + "&6&l" + value.name() + " &c&lNOT FOUND"));
                }
            }
        }

        if (PluginSupport.PLACEHOLDERAPI.isPluginEnabled()) {
            debug(() -> "PlaceholderAPI support is enabled!", Level.INFO);
            new PlaceholderAPISupport().register();
        }

        debug(() -> "You can disable logging by going to the plugin-config.yml and setting verbose to false.", Level.INFO);
    }

    @Override
    public void onDisable() {
        // End all crates.
        SessionManager.endCrates();

        // Remove quick crate rewards
        this.crazyHandler.getCrateManager().purgeRewards();

        // Purge holograms.
        if (this.crazyHandler.getCrateManager().getHolograms() != null) this.crazyHandler.getCrateManager().getHolograms().removeAllHolograms();

        // Unload the plugin.
        this.crazyHandler.unload();

        if (this.timer != null) this.timer.cancel();
    }

    @NotNull
    public BukkitCommandManager<CommandSender> getCommandManager() {
        return this.commandManager;
    }

    @NotNull
    public BukkitUserManager getUserManager() {
        return getCrazyHandler().getUserManager();
    }

    @NotNull
    public ConfigManager getConfigManager() {
        return getCrazyHandler().getConfigManager();
    }

    @NotNull
    public CrateManager getCrateManager() {
        return getCrazyHandler().getCrateManager();
    }

    @NotNull
    public FileManager getFileManager() {
        return getCrazyHandler().getFileManager();
    }

    @NotNull
    public CrazyHandler getCrazyHandler() {
        return this.crazyHandler;
    }

    @NotNull
    public Timer getTimer() {
        return this.timer;
    }

    public boolean isLogging() {
        return getConfigManager().getConfig().getProperty(ConfigKeys.verbose_logging);
    }

    private void registerPermissions() {
        Arrays.stream(Permissions.values()).toList().forEach(permission -> {
            Permission newPermission = new Permission(
                    permission.getPermission(),
                    permission.getDescription(),
                    permission.isDefault(),
                    permission.getChildren()
            );

            getServer().getPluginManager().addPermission(newPermission);
        });
    }

    public void debug(Supplier<String> message, Level level) {
        if (isLogging()) getLogger().log(level, message.get());
    }
}