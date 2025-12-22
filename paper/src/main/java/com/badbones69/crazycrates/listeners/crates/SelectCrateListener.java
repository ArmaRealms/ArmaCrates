package com.badbones69.crazycrates.listeners.crates;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.PrizeManager;
import com.badbones69.crazycrates.api.builders.CrateBuilder;
import com.badbones69.crazycrates.api.enums.Messages;
import com.badbones69.crazycrates.api.enums.PersistentKeys;
import com.badbones69.crazycrates.api.events.PlayerPrizeEvent;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.Prize;
import com.badbones69.crazycrates.api.objects.other.ItemBuilder;
import com.badbones69.crazycrates.api.utils.MiscUtils;
import com.badbones69.crazycrates.api.utils.MsgUtils;
import com.badbones69.crazycrates.tasks.crates.CrateManager;
import com.badbones69.crazycrates.tasks.crates.other.SelectCrateSession;
import com.badbones69.crazycrates.tasks.crates.types.SelectCrate;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import us.crazycrew.crazycrates.api.enums.types.CrateType;
import us.crazycrew.crazycrates.api.enums.types.KeyType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles inventory interactions for the SelectCrate type.
 */
public class SelectCrateListener implements Listener {

    @NotNull
    private final CrazyCrates plugin = CrazyCrates.get();

    @NotNull
    private final CrateManager crateManager = this.plugin.getCrateManager();

    // Store active sessions for players
    private final Map<UUID, SelectCrateSession> sessions = new HashMap<>();

    /**
     * Handles inventory clicks in SelectCrate GUIs.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder(false) instanceof CrateBuilder holder)) return;

        // Check if player is opening a SelectCrate
        Crate crate = this.crateManager.getOpeningCrate(player);
        if (crate == null || crate.getCrateType() != CrateType.select_crate) return;
        if (!this.crateManager.isInOpeningList(player)) return;

        // Cancel the event to prevent item movement
        event.setCancelled(true);

        // Get the raw slot clicked
        int slot = event.getRawSlot();
        InventoryView view = event.getView();
        Inventory topInventory = view.getTopInventory();

        // Check if clicking in the top inventory
        if (event.getClickedInventory() != topInventory) return;

        ItemStack clickedItem = topInventory.getItem(slot);
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // Get or create session
        SelectCrateSession session = sessions.computeIfAbsent(player.getUniqueId(), 
            k -> new SelectCrateSession(player, crate));

        // Check if clicked the confirm button
        int confirmSlot = crate.getFile() != null ? 
            crate.getFile().getInt("Crate.SelectCrate.Confirm.Slot", 49) : 49;

        if (slot == confirmSlot) {
            handleConfirmClick(player, session, view);
            return;
        }

        // Check if clicked on a prize
        ItemMeta itemMeta = clickedItem.getItemMeta();
        if (itemMeta == null) return;

        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        if (!container.has(PersistentKeys.crate_prize.getNamespacedKey())) return;

        String prizeName = container.get(PersistentKeys.crate_prize.getNamespacedKey(), PersistentDataType.STRING);
        Prize prize = crate.getPrize(prizeName);

        if (prize == null) return;

        // Handle prize selection
        handlePrizeSelection(player, session, prize, slot, topInventory, crate);
    }

    /**
     * Handles prize selection by the player.
     */
    private void handlePrizeSelection(Player player, SelectCrateSession session, Prize prize, 
                                      int slot, Inventory inventory, Crate crate) {
        // Clear previous selection marker if any
        if (session.hasSelection()) {
            int previousSlot = session.getSelectedSlot();
            Prize previousPrize = session.getSelectedPrize();
            if (previousPrize != null) {
                inventory.setItem(previousSlot, previousPrize.getDisplayItem(player));
            }
        }

        // Set new selection
        session.setSelectedPrize(prize, slot);

        // Add visual marker (add glow enchantment)
        ItemStack displayItem = prize.getDisplayItem(player);
        ItemBuilder builder = ItemBuilder.convertItemStack(displayItem);
        builder.setGlow(true);
        
        // Add selection marker in lore
        List<String> currentLore = new ArrayList<>(builder.getLore());
        currentLore.add("");
        
        ItemBuilder markerBuilder = SelectCrate.getSelectionMarker(crate.getFile());
        currentLore.add(markerBuilder.getName());
        
        builder.setLore(currentLore);
        
        inventory.setItem(slot, builder.build());

        // Play a sound
        crate.playSound(player, player.getLocation(), "click-sound", "UI_BUTTON_CLICK", SoundCategory.PLAYERS);
    }

    /**
     * Handles the confirm button click.
     */
    private void handleConfirmClick(Player player, SelectCrateSession session, InventoryView view) {
        Crate crate = session.getCrate();

        // Check if player has selected a prize
        if (!session.hasSelection()) {
            String message = crate.getFile() != null ?
                crate.getFile().getString("Crate.SelectCrate.Messages.NoSelection", "&cSelecione um prêmio antes de confirmar.") :
                "&cSelecione um prêmio antes de confirmar.";
            player.sendMessage(MsgUtils.color(message));
            return;
        }

        UUID uuid = player.getUniqueId();
        String crateName = crate.getName();
        KeyType type = this.crateManager.getPlayerKeyType(player);

        // Validate player still has keys
        boolean hasPhysicalKey = type == KeyType.physical_key && 
            this.plugin.getUserManager().hasPhysicalKey(uuid, crateName, this.crateManager.getHand(player));

        if (type == KeyType.physical_key && !hasPhysicalKey) {
            player.sendMessage(Messages.no_keys.getMessage(player));
            cleanupSession(player);
            player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
            return;
        }

        // Take the key
        boolean keyTaken = this.crateManager.hasPlayerKeyType(player) && 
            !this.plugin.getUserManager().takeKeys(
                crate.getRequiredKeys() > 0 ? crate.getRequiredKeys() : 1, 
                uuid, 
                crateName, 
                type, 
                this.crateManager.getHand(player)
            );

        if (!keyTaken) {
            MiscUtils.failedToTakeKey(player, crate);
            cleanupSession(player);
            player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
            return;
        }

        // Give the selected prize
        Prize selectedPrize = session.getSelectedPrize();
        PrizeManager.givePrize(player, selectedPrize, crate);

        // Fire the prize event
        this.plugin.getServer().getPluginManager().callEvent(
            new PlayerPrizeEvent(player, crate, crateName, selectedPrize));

        // Play sound
        crate.playSound(player, player.getLocation(), "stop-sound", "BLOCK_ANVIL_PLACE", SoundCategory.PLAYERS);

        // Cleanup and close
        cleanupSession(player);
        player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);

        // Send success messages from the prize
        for (String message : crate.getPrizeMessage()) {
            player.sendMessage(MsgUtils.color(message
                .replace("%crate%", crate.getName())
                .replace("%prize%", selectedPrize.getPrizeName())
                .replace("%player%", player.getName())));
        }
    }

    /**
     * Handles inventory close events for SelectCrate.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Crate crate = this.crateManager.getOpeningCrate(player);
        if (crate == null || crate.getCrateType() != CrateType.select_crate) return;

        // Only cleanup if not closing due to plugin (which means it was a manual close)
        if (event.getReason() != InventoryCloseEvent.Reason.PLUGIN) {
            cleanupSession(player);
        }
    }

    /**
     * Prevents dragging items in SelectCrate GUIs.
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Crate crate = this.crateManager.getOpeningCrate(player);
        if (crate == null || crate.getCrateType() != CrateType.select_crate) return;

        event.setCancelled(true);
    }

    /**
     * Cleans up sessions when players quit.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Crate crate = this.crateManager.getOpeningCrate(player);
        
        if (crate != null && crate.getCrateType() == CrateType.select_crate) {
            cleanupSession(player);
        }
    }

    /**
     * Cleans up a player's SelectCrate session.
     */
    private void cleanupSession(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Remove session
        sessions.remove(uuid);
        
        // Remove from opening list
        this.crateManager.removePlayerFromOpeningList(player);
        this.crateManager.removePlayerKeyType(player);
        this.crateManager.removeHands(player);
    }

    /**
     * Cleans up all sessions (called on plugin disable).
     */
    public void cleanupAllSessions() {
        sessions.clear();
    }
}
