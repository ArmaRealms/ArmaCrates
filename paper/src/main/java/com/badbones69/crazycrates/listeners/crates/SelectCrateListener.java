package com.badbones69.crazycrates.listeners.crates;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.PrizeManager;
import com.badbones69.crazycrates.api.builders.types.CratePrizeMenu;
import com.badbones69.crazycrates.api.enums.Messages;
import com.badbones69.crazycrates.api.events.PlayerPrizeEvent;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.Prize;
import com.badbones69.crazycrates.api.objects.other.ItemBuilder;
import com.badbones69.crazycrates.api.utils.ItemUtils;
import com.badbones69.crazycrates.api.utils.MiscUtils;
import com.badbones69.crazycrates.api.utils.MsgUtils;
import com.badbones69.crazycrates.tasks.crates.CrateManager;
import com.badbones69.crazycrates.tasks.crates.other.SelectCrateInventory;
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
import org.jetbrains.annotations.NotNull;
import us.crazycrew.crazycrates.api.enums.types.CrateType;
import us.crazycrew.crazycrates.api.enums.types.KeyType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

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
    private final Set<SelectCrateSession> pendingConfirmations = new HashSet<>();

    /**
     * Handles inventory clicks in SelectCrate GUIs.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof final Player player)) {
            this.plugin.debug(() -> "Non-player entity tried to interact with a SelectCrate inventory.");
            return;
        }

        final Inventory inventory = event.getInventory();
        if (!(inventory.getHolder(false) instanceof CratePrizeMenu)) {
            this.plugin.debug(() -> "Inventory holder is not a CratePrizeMenu.");
            return;
        }

        // Check if player is opening a SelectCrate
        final Crate crate = this.crateManager.getOpeningCrate(player);
        if (crate == null || crate.getCrateType() != CrateType.select_crate) {
            this.plugin.debug(() -> "Player is not opening a SelectCrate.");
            return;
        }

        if (!this.crateManager.isInOpeningList(player)) {
            this.plugin.debug(() -> "Player is not in the opening list for SelectCrate.");
            return;
        }

        // Cancel the event to prevent item movement
        event.setCancelled(true);

        if (!isCurrentMenu(crate, inventory)) {
            final SelectCrateSession session = sessions.get(player.getUniqueId());
            if (session != null) session.cancel();
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (player.getOpenInventory().getTopInventory() == inventory) closeSession(player);
            });
            return;
        }

        // Get the raw slot clicked
        final int slot = event.getRawSlot();
        final InventoryView view = event.getView();
        final Inventory topInventory = view.getTopInventory();

        // Check if clicking in the top inventory
        if (event.getClickedInventory() != topInventory) {
            return;
        }

        final ItemStack clickedItem = topInventory.getItem(slot);
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // Get or create session
        final SelectCrateSession session = sessions.computeIfAbsent(player.getUniqueId(),
                k -> new SelectCrateSession(player, crate));

        final int cancelSlot = SelectCrate.getControlSlot(crate, topInventory.getSize(), "Cancel");
        if (slot == cancelSlot) {
            session.cancel();
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (sessions.get(player.getUniqueId()) == session) closeSession(player);
            });
            return;
        }

        // Check if clicked the confirm button
        final int confirmSlot = SelectCrate.getControlSlot(crate, topInventory.getSize(), "Confirm");

        if (slot == confirmSlot) {
            if (!pendingConfirmations.add(session)) return;
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                try {
                    if (sessions.get(player.getUniqueId()) == session
                            && player.getOpenInventory().getTopInventory() == topInventory) {
                        handleConfirmClick(player, session, view);
                    }
                } finally {
                    pendingConfirmations.remove(session);
                }
            });
            this.plugin.debug(() -> "Player clicked the confirm button in SelectCrate.");
            return;
        }

        removeBlockedSelections(player, session);
        final int pageCount = SelectCrate.getPageCount(crate, topInventory.getSize());
        if (pageCount > 1) {
            final int previousSlot = SelectCrate.getControlSlot(crate, topInventory.getSize(), "Navigation.Previous");
            final int nextSlot = SelectCrate.getControlSlot(crate, topInventory.getSize(), "Navigation.Next");

            if (slot == previousSlot || slot == nextSlot) {
                handleNavigationClick(player, session, topInventory, slot == nextSlot, pageCount);
                return;
            }
        }

        // Resolve by page and slot; display items and selection markers are not identities.
        final int index = session.getCurrentPage() * SelectCrate.getPrizesPerPage(topInventory.getSize()) + slot;
        if (slot >= SelectCrate.getPrizesPerPage(topInventory.getSize()) || index >= crate.getPrizes().size()) return;
        final Prize prize = crate.getPrizes().get(index);
        final int balance = availableKeys(player, crate);
        final boolean free = this.crateManager.getPlayerKeyType(player) == KeyType.free_key;
        if (prize.hasPermission(player)) {
            session.togglePrize(prize, 0);
            player.sendMessage(SelectCrate.message(crate, "BlockedPrize", "&cVocê não pode selecionar este prêmio."));
        } else if (!session.togglePrize(prize, balance, free)) {
            final int max = SelectCrateSession.getMaxSelections(crate, free);
            player.sendMessage(session.getSelectedPrizes().size() >= max
                    ? SelectCrate.message(crate, "MaxSelections", "&cVocê pode selecionar no máximo %max% prêmios.")
                            .replace("%max%", Integer.toString(max))
                    : SelectCrate.message(crate, "SelectionLimit", "&cVocê não possui chaves suficientes para esta seleção. Desmarque outro produto."));
        }
        render(player, session, topInventory);
        crate.playSound(player, player.getLocation(), "click-sound", "UI_BUTTON_CLICK", SoundCategory.PLAYERS);
    }

    private boolean isCurrentMenu(final Crate crate, final Inventory inventory) {
        return inventory.getHolder(false) instanceof CratePrizeMenu holder
                && holder.getCrate() == crate && holder.hasCurrentPrizes()
                && this.crateManager.getCrateFromName(crate.getName()) == crate;
    }

    private int availableKeys(final Player player, final Crate crate) {
        return SelectCrate.getAvailableKeys(player, crate, this.crateManager.getPlayerKeyType(player),
                this.crateManager.getHand(player));
    }

    private void render(final Player player, final SelectCrateSession session, final Inventory inventory) {
        SelectCrate.renderPage(inventory, player, session.getCrate(), session.getCurrentPage(),
                session.getSelectedPrizes(), availableKeys(player, session.getCrate()),
                this.crateManager.getPlayerKeyType(player) == KeyType.free_key);
    }

    /**
     * Moves the session to an adjacent page and redraws its prizes and controls.
     *
     * @param player    player navigating the selection GUI
     * @param session   active selection session
     * @param inventory inventory to redraw
     * @param next      whether to move forward instead of backward
     * @param pageCount total number of available pages
     */
    private void handleNavigationClick(final Player player, final SelectCrateSession session,
                                       final Inventory inventory, final boolean next, final int pageCount) {
        final int offset = next ? 1 : -1;
        final int destination = Math.clamp(session.getCurrentPage() + offset, 0, pageCount - 1);
        if (destination == session.getCurrentPage()) {
            return;
        }

        session.setCurrentPage(destination);
        render(player, session, inventory);
        session.getCrate().playSound(player, player.getLocation(), "click-sound", "UI_BUTTON_CLICK", SoundCategory.PLAYERS);
    }

    /**
     * Handles the confirm button click.
     */
    private void handleConfirmClick(final Player player, final SelectCrateSession session, final InventoryView view) {
        final Crate crate = session.getCrate();

        if (rejectBlockedSelection(player, session, view.getTopInventory())
                || rejectSelectionLimit(player, session, view.getTopInventory())) return;

        // Check if player has selected a prize
        if (!session.hasSelection()) {
            final String message = crate.getFile() != null ?
                    crate.getFile().getString("Crate.SelectCrate.Messages.NoSelection", "&cPlease select a prize before confirming.") :
                    "&cPlease select a prize before confirming.";
            player.sendMessage(MsgUtils.color(message));
            return;
        }

        final List<Prize> selected = List.copyOf(session.getSelectedPrizes());
        final KeyType type = this.crateManager.getPlayerKeyType(player);
        if (type == null) {
            closeSession(player);
            return;
        }
        if (SelectCrateSession.getSelectionCost(crate, selected, type == KeyType.free_key) > availableKeys(player, crate)) {
            player.sendMessage(Messages.no_keys.getMessage(player));
            render(player, session, view.getTopInventory());
            return;
        }
        if (!isCurrentMenu(crate, view.getTopInventory()) || !crate.getPrizes().containsAll(selected)) {
            closeSession(player);
            return;
        }

        final List<ItemStack> items = new ArrayList<>();
        final SelectCrateInventory candidate;
        final int cost;
        try {
            for (final Prize prize : selected) {
                for (final ItemStack item : prize.getItems()) {
                    if (item == null) throw new IllegalArgumentException("Missing reward item");
                    items.add(item.clone());
                }
                for (final ItemBuilder builder : prize.getItemBuilders()) {
                    items.add(new ItemBuilder(builder).setTarget(player).build());
                }
            }
            if (!player.hasPermission("crazycrates.open." + crate.getName())
                    || !player.hasPermission("crazycrates.open." + crate.getName().toLowerCase(Locale.ROOT))) {
                player.sendMessage(Messages.no_crate_permission.getMessage(player));
                closeSession(player);
                return;
            }
            if (!isCurrentMenu(crate, view.getTopInventory()) || !crate.getPrizes().containsAll(selected)) {
                closeSession(player);
                return;
            }
            if (rejectBlockedSelection(player, session, view.getTopInventory())
                    || rejectSelectionLimit(player, session, view.getTopInventory())) return;
            if (SelectCrateSession.getSelectionCost(crate, selected, type == KeyType.free_key) > availableKeys(player, crate)) {
                player.sendMessage(Messages.no_keys.getMessage(player));
                return;
            }
            cost = type == KeyType.free_key ? 0 : Math.toIntExact(SelectCrateSession.getSelectionCost(crate, selected, false));
            // Item builders and permission checks may call integrations; capture inventory afterwards.
            candidate = new SelectCrateInventory(player.getInventory());
            if (type == KeyType.physical_key && !candidate.takeKeys(cost,
                    item -> ItemUtils.isSimilar(item, crate), this.crateManager.getHand(player))) {
                player.sendMessage(Messages.no_keys.getMessage(player));
                return;
            }
            if (!candidate.addItems(items)) {
                player.sendMessage(SelectCrate.message(crate, "InventoryFull",
                        "&cVocê não possui espaço suficiente no inventário para receber todos os produtos selecionados."));
                return;
            }
        } catch (final RuntimeException exception) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Cannot prepare SelectCrate rewards for " + crate.getName(), exception);
            player.sendMessage(SelectCrate.message(crate, "InvalidReward", "&cNão foi possível preparar os produtos. Avise a equipe."));
            return;
        }

        if (sessions.get(player.getUniqueId()) != session
                || player.getOpenInventory().getTopInventory() != view.getTopInventory()
                || !session.beginConfirmation()) return;
        final SelectCrateInventory original = new SelectCrateInventory(player.getInventory());
        final int originalKeys = this.plugin.getUserManager().getVirtualKeys(player.getUniqueId(), crate.getName());
        // No external callbacks occur between the final validation, key debit and item commit.
        try {
            if (type == KeyType.virtual_key && !this.plugin.getUserManager().takeKeys(cost,
                    player.getUniqueId(), crate.getName(), type, false)) {
                MiscUtils.failedToTakeKey(player, crate);
                return;
            }
            candidate.apply(player.getInventory());
        } catch (final RuntimeException exception) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "SelectCrate item commit failed: " + crate.getName(), exception);
            original.apply(player.getInventory());
            if (type == KeyType.virtual_key) {
                this.plugin.getUserManager().setKeys(originalKeys, player.getUniqueId(), crate.getName());
            }
            return;
        } finally {
            closeSession(player);
        }

        // shortcut: external commands cannot be rolled back; use Items for inventory-safe delivery.
        for (final Prize prize : selected) {
            runRewardCallback(crate, () -> PrizeManager.finishPrize(player, prize, crate));
            runRewardCallback(crate, () -> {
                if (prize.useFireworks()) MiscUtils.spawnFirework(player.getLocation().add(0, 1, 0), null);
            });
            runRewardCallback(crate, () -> this.plugin.getServer().getPluginManager()
                    .callEvent(new PlayerPrizeEvent(player, crate, crate.getName(), prize)));
        }
        crate.playSound(player, player.getLocation(), "stop-sound", "BLOCK_ANVIL_PLACE", SoundCategory.PLAYERS);
    }

    private boolean rejectSelectionLimit(final Player player, final SelectCrateSession session, final Inventory inventory) {
        final int max = SelectCrateSession.getMaxSelections(session.getCrate(),
                this.crateManager.getPlayerKeyType(player) == KeyType.free_key);
        if (session.getSelectedPrizes().size() <= max) return false;
        player.sendMessage(SelectCrate.message(session.getCrate(), "MaxSelections", "&cVocê pode selecionar no máximo %max% prêmios.")
                .replace("%max%", Integer.toString(max)));
        render(player, session, inventory);
        return true;
    }

    private boolean removeBlockedSelections(final Player player, final SelectCrateSession session) {
        boolean blocked = false;
        for (final Prize prize : session.getSelectedPrizes()) {
            if (prize.hasPermission(player)) {
                session.togglePrize(prize, 0);
                blocked = true;
            }
        }
        return blocked;
    }

    /** Removes newly blocked choices and rejects the whole confirmation before any debit. */
    private boolean rejectBlockedSelection(final Player player, final SelectCrateSession session, final Inventory inventory) {
        if (!removeBlockedSelections(player, session)) return false;
        player.sendMessage(SelectCrate.message(session.getCrate(), "BlockedPrize", "&cVocê não pode selecionar este prêmio."));
        render(player, session, inventory);
        return true;
    }

    private void runRewardCallback(final Crate crate, final Runnable callback) {
        try {
            callback.run();
        } catch (final RuntimeException exception) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "SelectCrate reward callback failed after item delivery: " + crate.getName(), exception);
        }
    }

    private void closeSession(final Player player) {
        cleanupSession(player);
        player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
    }

    /**
     * Handles inventory close events for SelectCrate.
     */
    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof final Player player)) return;

        final Crate crate = this.crateManager.getOpeningCrate(player);
        if (crate == null || crate.getCrateType() != CrateType.select_crate) return;

        if (event.getInventory().getHolder(false) instanceof CratePrizeMenu) cleanupSession(player);
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
}
