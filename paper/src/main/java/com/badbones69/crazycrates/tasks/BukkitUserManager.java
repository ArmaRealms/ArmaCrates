package com.badbones69.crazycrates.tasks;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.FileManager;
import com.badbones69.crazycrates.api.FileManager.Files;
import com.badbones69.crazycrates.api.enums.Messages;
import com.badbones69.crazycrates.api.events.PlayerReceiveKeyEvent;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.utils.ItemUtils;
import com.badbones69.crazycrates.api.utils.MiscUtils;
import com.badbones69.crazycrates.common.config.types.ConfigKeys;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import us.crazycrew.crazycrates.api.enums.types.CrateType;
import us.crazycrew.crazycrates.api.enums.types.KeyType;
import us.crazycrew.crazycrates.api.users.UserManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class BukkitUserManager extends UserManager {

    @NotNull
    private final CrazyCrates plugin = CrazyCrates.get();

    @NotNull
    private final FileManager fileManager = this.plugin.getFileManager();

    @NotNull
    private final FileConfiguration data = this.fileManager.getFile(Files.DATA);

    @Override
    public Player getUser(UUID uuid) {
        return this.plugin.getServer().getPlayer(uuid);
    }

    @Override
    public int getVirtualKeys(UUID uuid, String crateName) {
        synchronized (this.data) {
            return this.data.getInt("Players." + uuid + "." + crateName, 0);
        }
    }

    @Override
    public void addVirtualKeys(int amount, UUID uuid, String crateName) {
        if (isPlayerNull(uuid)) {
            this.plugin.debug(() -> "Player with the uuid: " + uuid + " is null.", Level.WARNING);
            return;
        }

        if (isCrateInvalid(crateName)) {
            this.plugin.debug(() -> "Crate " + crateName + " doesn't exist.", Level.WARNING);
            return;
        }

        Crate crate = this.plugin.getCrateManager().getCrateFromName(crateName);

        Player player = getUser(uuid);

        synchronized (this.data) {
            int keys = getVirtualKeys(uuid, crate.getName());

            if (!this.data.contains("Players." + uuid + ".Name")) {
                this.data.set("Players." + uuid + ".Name", player.getName());
            }

            this.data.set("Players." + uuid + "." + crate.getName(), (Math.max((keys + amount), 0)));
        }

        this.fileManager.queueSave(Files.DATA);
    }

    @Override
    public void setKeys(int amount, UUID uuid, String crateName) {
        if (isPlayerNull(uuid)) {
            this.plugin.debug(() -> "Player with the uuid: " + uuid + " is null.", Level.WARNING);
            return;
        }

        if (isCrateInvalid(crateName)) {
            this.plugin.debug(() -> "Crate " + crateName + " doesn't exist.", Level.WARNING);
            return;
        }

        Player player = getUser(uuid);

        Crate crate = this.plugin.getCrateManager().getCrateFromName(crateName);

        synchronized (this.data) {
            this.data.set("Players." + player.getUniqueId() + ".Name", player.getName());
            this.data.set("Players." + player.getUniqueId() + "." + crate.getName(), amount);
        }
        this.fileManager.queueSave(Files.DATA);
    }

    private boolean isPlayerNull(UUID uuid) {
        return getUser(uuid) == null;
    }

    @Override
    public void addKeys(int amount, UUID uuid, String crateName, KeyType keyType) {
        if (isPlayerNull(uuid)) {
            if (this.plugin.isLogging()) this.plugin.getLogger().warning("Player with the uuid " + uuid + " is null.");
            return;
        }

        if (isCrateInvalid(crateName)) {
            if (this.plugin.isLogging()) this.plugin.getLogger().warning("Crate " + crateName + " doesn't exist.");
            return;
        }

        Player player = getUser(uuid);

        Crate crate = this.plugin.getCrateManager().getCrateFromName(crateName);

        switch (keyType) {
            case physical_key -> {
                if (!MiscUtils.isInventoryFull(player)) {
                    player.getInventory().addItem(crate.getKey(amount, player));
                    return;
                }

                if (this.plugin.getConfigManager().getConfig().getProperty(ConfigKeys.give_virtual_keys_when_inventory_full)) {
                    addVirtualKeys(amount, player.getUniqueId(), crate.getName());

                    if (this.plugin.getConfigManager().getConfig().getProperty(ConfigKeys.notify_player_when_inventory_full)) {
                        HashMap<String, String> placeholders = new HashMap<>();
                        placeholders.put("%amount%", String.valueOf(amount));
                        placeholders.put("%key%", crate.getName());

                        player.sendMessage(Messages.cannot_give_player_keys.getMessage(placeholders, player));
                    }

                    return;
                }

                player.getWorld().dropItem(player.getLocation(), crate.getKey(amount, player));
            }

            case virtual_key -> addVirtualKeys(amount, player.getUniqueId(), crate.getName());
        }
    }

    @Override
    public int getTotalKeys(UUID uuid, String crateName) {
        return getVirtualKeys(uuid, crateName) + getPhysicalKeys(uuid, crateName);
    }

    @Override
    public int getPhysicalKeys(UUID uuid, String crateName) {
        if (isPlayerNull(uuid)) {
            if (this.plugin.isLogging()) this.plugin.getLogger().warning("Player with the uuid " + uuid + " is null.");
            return 0;
        }

        if (isCrateInvalid(crateName)) {
            if (this.plugin.isLogging()) this.plugin.getLogger().warning("Crate " + crateName + " doesn't exist.");
            return 0;
        }

        Player player = getUser(uuid);

        int keys = 0;

        Crate crate = this.plugin.getCrateManager().getCrateFromName(crateName);

        for (ItemStack item : player.getOpenInventory().getBottomInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;

            if (ItemUtils.isSimilar(item, crate)) keys += item.getAmount();
        }

        return keys;
    }

    @Override
    public boolean takeKeys(int amount, UUID uuid, String crateName, KeyType keyType, boolean checkHand) {
        if (isPlayerNull(uuid)) {
            if (this.plugin.isLogging()) this.plugin.getLogger().warning("Player with the uuid " + uuid + " is null.");
            return false;
        }

        if (isCrateInvalid(crateName)) {
            if (this.plugin.isLogging()) this.plugin.getLogger().warning("Crate " + crateName + " doesn't exist.");
            return false;
        }

        Player player = getUser(uuid);

        Crate crate = this.plugin.getCrateManager().getCrateFromName(crateName);

        switch (keyType) {
            case physical_key -> {
                int takeAmount = amount;

                try {
                    List<ItemStack> items = new ArrayList<>();

                    if (checkHand) {
                        items.add(player.getEquipment().getItemInMainHand());
                        items.add(player.getEquipment().getItemInOffHand());
                    } else {
                        items.addAll(Arrays.asList(player.getInventory().getContents()));
                        items.remove(player.getEquipment().getItemInOffHand());
                    }

                    for (ItemStack item : items) {
                        if (item != null && (isKeyFromCrate(item, crate))) {
                            int keyAmount = item.getAmount();

                            if ((takeAmount - keyAmount) >= 0) {
                                MiscUtils.removeMultipleItemStacks(player.getInventory(), item);
                                takeAmount -= keyAmount;
                            } else {
                                item.setAmount(keyAmount - takeAmount);
                                takeAmount = 0;
                            }

                            if (takeAmount <= 0) return true;
                        }
                    }

                    // This needs to be done as player.getInventory().removeItem(ItemStack); does NOT remove from the offhand.
                    if (takeAmount > 0) {
                        ItemStack item = player.getEquipment().getItemInOffHand();

                        if (isKeyFromCrate(item, crate)) {
                            int keyAmount = item.getAmount();

                            if ((takeAmount - keyAmount) >= 0) {
                                player.getEquipment().setItemInOffHand(new ItemStack(Material.AIR, 1));
                                takeAmount -= keyAmount;
                            } else {
                                item.setAmount(keyAmount - takeAmount);
                                takeAmount = 0;
                            }

                            if (takeAmount <= 0) return true;
                        }
                    }
                } catch (Exception exception) {
                    MiscUtils.failedToTakeKey(player, crate);
                    return false;
                }
            }

            case virtual_key -> {
                synchronized (this.data) {
                    int keys = getVirtualKeys(uuid, crate.getName());

                    this.data.set("Players." + uuid + ".Name", player.getName());

                    int newAmount = Math.max((keys - amount), 0);

                    if (newAmount < 1) {
                        this.data.set("Players." + uuid + "." + crate.getName(), null);
                    } else {
                        this.data.set("Players." + uuid + "." + crate.getName(), newAmount);
                    }
                }

                this.fileManager.queueSave(Files.DATA);
                return true;
            }

            case free_key -> {
                return true;
            }
        }

        MiscUtils.failedToTakeKey(player, crate);
        return false;
    }

    @Override
    public boolean hasPhysicalKey(UUID uuid, String crateName, boolean checkHand) {
        if (isPlayerNull(uuid)) {
            if (this.plugin.isLogging()) this.plugin.getLogger().warning("Player with the uuid " + uuid + " is null.");
            return false;
        }

        if (isCrateInvalid(crateName)) {
            if (this.plugin.isLogging()) this.plugin.getLogger().warning("Crate " + crateName + " doesn't exist.");
            return false;
        }

        Player player = getUser(uuid);

        Crate crate = this.plugin.getCrateManager().getCrateFromName(crateName);

        List<ItemStack> items = new ArrayList<>();

        if (checkHand) {
            items.add(player.getEquipment().getItemInMainHand());
            items.add(player.getEquipment().getItemInOffHand());
        } else {
            items.addAll(Arrays.asList(player.getInventory().getContents()));
            items.removeAll(Arrays.asList(player.getInventory().getArmorContents()));
        }

        for (ItemStack item : items) {
            if (item != null) {
                if (isKeyFromCrate(item, crate)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if a key is from a specific Crate.
     *
     * @param item  The key item you are checking.
     * @param crate The crate you are checking.
     * @return true if it belongs to that crate and false if it does not.
     */
    private boolean isKeyFromCrate(ItemStack item, Crate crate) {
        if (crate.getCrateType() != CrateType.menu && (item != null && item.getType() != Material.AIR)) {
            return ItemUtils.isSimilar(item, crate);
        }

        return false;
    }

    @Override
    public boolean addOfflineKeys(UUID uuid, String crateName, int keys, KeyType keyType) {
        if (isCrateInvalid(crateName)) {
            if (this.plugin.isLogging()) this.plugin.getLogger().warning("Crate " + crateName + " doesn't exist.");
            return false;
        }

        Crate crate = this.plugin.getCrateManager().getCrateFromName(crateName);

        try {
            final String path = "Offline-Players." + uuid + "."
                    + (keyType == KeyType.physical_key ? "Physical." : "") + crate.getName();
            synchronized (this.data) {
                this.data.set(path, this.data.getInt(path) + keys);
            }
            this.fileManager.queueSave(Files.DATA);
            return true;
        } catch (Exception exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not add keys to offline player with uuid: " + uuid, exception);
            return false;
        }
    }

    @Override
    public boolean takeOfflineKeys(UUID uuid, String crateName, int keys, KeyType keyType) {
        if (isCrateInvalid(crateName)) {
            if (this.plugin.isLogging()) this.plugin.getLogger().warning("Crate " + crateName + " doesn't exist.");
            return false;
        }

        Crate crate = this.plugin.getCrateManager().getCrateFromName(crateName);

        try {
            final boolean physical = keyType == KeyType.physical_key;
            final String path = "Offline-Players." + uuid + "." + (physical ? "Physical." : "") + crate.getName();
            synchronized (this.data) {
                final int balance = this.data.getInt(path);
                final int remaining = balance - (physical ? Math.min(balance, keys) : keys);
                this.data.set(path, physical && remaining <= 0 ? null : remaining);
            }
            this.fileManager.queueSave(Files.DATA);
            return true;
        } catch (Exception exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not take keys from offline player with uuid: " + uuid, exception);
            return false;
        }
    }

    private boolean containsData(final String path) {
        synchronized (this.data) {
            return this.data.contains(path);
        }
    }

    public void loadOldOfflinePlayersKeys(Player player, List<Crate> crates) {
        String name = player.getName().toLowerCase();

        if (containsData("Offline-Players." + name)) {
            for (Crate crate : crates) {
                if (containsData("Offline-Players." + name + "." + crate.getName())) {
                    PlayerReceiveKeyEvent event = new PlayerReceiveKeyEvent(player, crate, PlayerReceiveKeyEvent.KeyReceiveReason.OFFLINE_PLAYER, 1);
                    this.plugin.getServer().getPluginManager().callEvent(event);

                    if (!event.isCancelled()) {
                        synchronized (this.data) {
                            final String pending = "Offline-Players." + name + "." + crate.getName();
                            final int keys = getVirtualKeys(player.getUniqueId(), crate.getName());
                            final int addedKeys = this.data.getInt(pending);

                            this.data.set("Players." + player.getUniqueId() + "." + crate.getName(), Math.max(keys + addedKeys, 0));
                            this.data.set(pending, null);
                        }

                        this.fileManager.queueSave(Files.DATA);
                    }
                }
            }

            synchronized (this.data) {
                this.data.set("Offline-Players." + name, null);
            }

            this.fileManager.queueSave(Files.DATA);
        }
    }

    /**
     * Load the offline keys of a player who has come online.
     *
     * @param player The player which you would like to load the offline keys for.
     */
    public void loadOfflinePlayersKeys(Player player, List<Crate> crates) {
        if (!containsData("Offline-Players." + player.getUniqueId()) || crates.isEmpty()) return;

        UUID uuid = player.getUniqueId();

        for (Crate crate : crates) {
            if (containsData("Offline-Players." + uuid + "." + crate.getName())) {
                PlayerReceiveKeyEvent event = new PlayerReceiveKeyEvent(player, crate, PlayerReceiveKeyEvent.KeyReceiveReason.OFFLINE_PLAYER, 1);
                this.plugin.getServer().getPluginManager().callEvent(event);

                if (event.isCancelled()) return;

                synchronized (this.data) {
                    if (this.data.contains("Offline-Players." + uuid + "." + crate.getName())) {
                        int keysGiven = 0;

                        int amount = this.data.getInt("Offline-Players." + uuid + "." + crate.getName());

                        //TODO() Instead of dropping the keys, make it so they need to empty their inventory and prompt them to open a gui.

                        while (keysGiven < amount) {
                            // If the inventory is full, drop the remaining keys then stop.
                            if (crate.getCrateType() == CrateType.crate_on_the_go) {
                                // If the inventory is full, drop the items then stop.
                                if (MiscUtils.isInventoryFull(player)) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), crate.getKey(amount, player));
                                    break;
                                }
                            }

                            keysGiven++;
                        }

                        // If the crate type is on the go.
                        if (crate.getCrateType() == CrateType.crate_on_the_go) {
                            // If the inventory not full, add to inventory.
                            player.getInventory().addItem(crate.getKey(amount, player));
                        } else {
                            // Otherwise add virtual keys.
                            addVirtualKeys(amount, uuid, crate.getName());
                        }

                        // If keys given is greater or equal than, remove data.
                        if (keysGiven >= amount) this.data.set("Offline-Players." + uuid + "." + crate.getName(), null);
                    }
                }
                this.fileManager.queueSave(Files.DATA);
            }

            if (containsData("Offline-Players." + uuid + ".Physical." + crate.getName())) {
                PlayerReceiveKeyEvent event = new PlayerReceiveKeyEvent(player, crate, PlayerReceiveKeyEvent.KeyReceiveReason.OFFLINE_PLAYER, 1);
                this.plugin.getServer().getPluginManager().callEvent(event);

                if (event.isCancelled()) return;

                synchronized (this.data) {
                    if (this.data.contains("Offline-Players." + uuid + ".Physical." + crate.getName())) {
                        int keysGiven = 0;

                        int amount = this.data.getInt("Offline-Players." + uuid + ".Physical." + crate.getName());

                        while (keysGiven < amount) {
                            // If the inventory is full, drop the remaining keys then stop.
                            if (MiscUtils.isInventoryFull(player)) {
                                player.getWorld().dropItemNaturally(player.getLocation(), crate.getKey(amount - keysGiven, player));
                                break;
                            }

                            keysGiven++;
                        }

                        // If the inventory not full, add to inventory.
                        player.getInventory().addItem(crate.getKey(keysGiven, player));

                        // If keys given is greater or equal than, remove data.
                        if (keysGiven >= amount)
                            this.data.set("Offline-Players." + uuid + ".Physical." + crate.getName(), null);
                    }
                }
                this.fileManager.queueSave(Files.DATA);
            }
        }

        synchronized (this.data) {
            ConfigurationSection physicalSection = this.data.getConfigurationSection("Offline-Players." + uuid + ".Physical");

            if (physicalSection != null && physicalSection.getKeys(false).isEmpty()) {
                this.data.set("Offline-Players." + uuid + ".Physical", null);
            }

            ConfigurationSection section = this.data.getConfigurationSection("Offline-Players." + uuid);

            if (section != null) {
                if (section.getKeys(false).isEmpty()) this.data.set("Offline-Players." + uuid, null);
            }
        }

        this.fileManager.queueSave(Files.DATA);
    }


    private boolean isCrateInvalid(String crateName) {
        return crateName.isBlank() || this.plugin.getCrateManager().getCrateFromName(crateName) == null;
    }
}