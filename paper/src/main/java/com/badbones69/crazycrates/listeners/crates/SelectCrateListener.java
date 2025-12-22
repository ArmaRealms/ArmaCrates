package com.badbones69.crazycrates.listeners.crates;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.PrizeManager;
import com.badbones69.crazycrates.api.builders.CrateBuilder;
import com.badbones69.crazycrates.api.builders.types.CratePrizeMenu;
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
import org.bukkit.Bukkit;
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
import java.util.Objects;
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
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof final Player player)) {
            this.plugin.debug(() ->"Non-player entity tried to interact with a SelectCrate inventory.");
            return;
        }

        final Inventory inventory = event.getInventory();
        if (!(inventory.getHolder(false) instanceof CratePrizeMenu)) {
            this.plugin.debug(() ->"Inventory holder is not a CratePrizeMenu.");
            return;
        }

        // Check if player is opening a SelectCrate
        final Crate crate = this.crateManager.getOpeningCrate(player);
        if (crate == null || crate.getCrateType() != CrateType.select_crate) {
            this.plugin.debug(() ->"Player is not opening a SelectCrate.");
            return;
        }

        if (!this.crateManager.isInOpeningList(player)) {
            this.plugin.debug(() ->"Player is not in the opening list for SelectCrate.");
            return;
        }

        // Cancel the event to prevent item movement
        event.setCancelled(true);

        // Get the raw slot clicked
        final int slot = event.getRawSlot();
        final InventoryView view = event.getView();
        final Inventory topInventory = view.getTopInventory();

        // Check if clicking in the top inventory
        if (event.getClickedInventory() != topInventory) {
            this.plugin.debug(() -> "Clicked inventory is not the top inventory in SelectCrate.");
            return;
        }

        final ItemStack clickedItem = topInventory.getItem(slot);
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            this.plugin.debug(() ->"Clicked item is null or air in SelectCrate.");
            return;
        }

        // Get or create session
        final SelectCrateSession session = sessions.computeIfAbsent(player.getUniqueId(),
                k -> new SelectCrateSession(player, crate));

        // Check if clicked the confirm button
        final int confirmSlot = crate.getFile() != null ?
                crate.getFile().getInt("Crate.SelectCrate.Confirm.Slot", 49) : 49;

        if (slot == confirmSlot) {
            handleConfirmClick(player, session, view);
            this.plugin.debug(() ->"Player clicked the confirm button in SelectCrate.");
            return;
        }

        // Check if clicked on a prize
        final Prize prize = crate.getPrize(clickedItem);
        if (prize == null) {
            this.plugin.debug(() ->"Clicked item is not a valid prize in SelectCrate.");
            return;
        }

        // Handle prize selection
        handlePrizeSelection(player, session, prize, slot, topInventory, crate);
    }

    /**
     * Handles prize selection by the player.
     */
    private void handlePrizeSelection(final Player player, final SelectCrateSession session, final Prize prize,
                                      final int slot, final Inventory inventory, final Crate crate) {
        // Clear previous selection marker if any
        if (session.hasSelection()) {
            final int previousSlot = session.getSelectedSlot();
            final Prize previousPrize = session.getSelectedPrize();
            if (previousPrize != null) {
                inventory.setItem(previousSlot, previousPrize.getDisplayItem(player));
            }
        }

        // Set new selection
        session.setSelectedPrize(prize, slot);

        // Add visual marker (add glow enchantment)
        final ItemStack displayItem = prize.getDisplayItem(player);
        final ItemBuilder builder = ItemBuilder.convertItemStack(displayItem);
        builder.setGlow(true);

        // Add selection marker in lore
        final List<String> currentLore = new ArrayList<>(builder.getLore());
        currentLore.add("");

        final ItemBuilder markerBuilder = SelectCrate.getSelectionMarker(crate.getFile());
        currentLore.add(markerBuilder.getName());

        builder.setLore(currentLore);

        inventory.setItem(slot, builder.build());

        // Play a sound
        crate.playSound(player, player.getLocation(), "click-sound", "UI_BUTTON_CLICK", SoundCategory.PLAYERS);
    }

    /**
     * Handles the confirm button click.
     */
    private void handleConfirmClick(final Player player, final SelectCrateSession session, final InventoryView view) {
        final Crate crate = session.getCrate();

        // Check if player has selected a prize
        if (!session.hasSelection()) {
            final String message = crate.getFile() != null ?
                    crate.getFile().getString("Crate.SelectCrate.Messages.NoSelection", "&cPlease select a prize before confirming.") :
                    "&cPlease select a prize before confirming.";
            player.sendMessage(MsgUtils.color(message));
            return;
        }

        final UUID uuid = player.getUniqueId();
        final String crateName = crate.getName();
        final KeyType type = this.crateManager.getPlayerKeyType(player);

        // Validate player still has keys (only for physical keys)
        if (type == KeyType.physical_key) {
            final boolean hasPhysicalKey = this.plugin.getUserManager().hasPhysicalKey(uuid, crateName, this.crateManager.getHand(player));

            if (!hasPhysicalKey) {
                player.sendMessage(Messages.no_keys.getMessage(player));
                cleanupSession(player);
                player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                return;
            }
        }

        // Take the key - takeKeys returns true on success, false on failure
        final boolean failedToTakeKeys = this.crateManager.hasPlayerKeyType(player) &&
                !this.plugin.getUserManager().takeKeys(
                        crate.getRequiredKeys() > 0 ? crate.getRequiredKeys() : 1,
                        uuid,
                        crateName,
                        type,
                        this.crateManager.getHand(player)
                );

        if (failedToTakeKeys) {
            MiscUtils.failedToTakeKey(player, crate);
            cleanupSession(player);
            player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
            return;
        }

        // Give the selected prize
        final Prize selectedPrize = session.getSelectedPrize();
        PrizeManager.givePrizeAndCallEvent(player, crate, selectedPrize);

        // Play sound
        crate.playSound(player, player.getLocation(), "stop-sound", "BLOCK_ANVIL_PLACE", SoundCategory.PLAYERS);

        // Cleanup and close
        cleanupSession(player);
        player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);

        // Send success messages from the prize
        for (final String message : crate.getPrizeMessage()) {
            player.sendMessage(MsgUtils.color(message
                    .replace("%crate%", crate.getName())
                    .replace("%prize%", Objects.requireNonNull(selectedPrize).getPrizeName())
                    .replace("%player%", player.getName())));
        }
    }

    /**
     * Handles inventory close events for SelectCrate.
     */
    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof final Player player)) return;

        final Crate crate = this.crateManager.getOpeningCrate(player);
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
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof final Player player)) return;

        final Crate crate = this.crateManager.getOpeningCrate(player);
        if (crate == null || crate.getCrateType() != CrateType.select_crate) return;

        event.setCancelled(true);
    }

    /**
     * Cleans up sessions when players quit.
     */
    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final Crate crate = this.crateManager.getOpeningCrate(player);

        if (crate != null && crate.getCrateType() == CrateType.select_crate) {
            cleanupSession(player);
        }
    }

    /**
     * Cleans up a player's SelectCrate session.
     */
    private void cleanupSession(final Player player) {
        final UUID uuid = player.getUniqueId();

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
