package com.badbones69.crazycrates.tasks.crates.other;

import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.Prize;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a player's session when opening a SelectCrate.
 * Tracks selected prizes, pagination and confirmation state.
 */
public class SelectCrateSession {

    private final UUID playerUUID;
    private final Crate crate;
    private final Set<Prize> selectedPrizes = new LinkedHashSet<>();
    private boolean confirming;
    private int currentPage;

    /**
     * Creates a new SelectCrate session for a player.
     *
     * @param player the player opening the crate
     * @param crate  the crate being opened
     */
    public SelectCrateSession(@NotNull Player player, @NotNull Crate crate) {
        this.playerUUID = player.getUniqueId();
        this.crate = crate;
        this.currentPage = 0;
    }

    /**
     * @return the UUID of the player
     */
    @NotNull
    public UUID getPlayerUUID() {
        return this.playerUUID;
    }

    /**
     * @return the crate being opened
     */
    @NotNull
    public Crate getCrate() {
        return this.crate;
    }

    /** Returns an immutable snapshot in selection order. */
    public Set<Prize> getSelectedPrizes() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(this.selectedPrizes));
    }

    /** Toggles a prize without allowing the current key limit to be exceeded. */
    public boolean togglePrize(final Prize prize, final int limit) {
        if (this.confirming) return false;
        if (this.selectedPrizes.remove(prize)) return true;
        return this.selectedPrizes.size() < limit && this.selectedPrizes.add(prize);
    }

    /** Invalidates queued confirmations as soon as Cancel is clicked. */
    public void cancel() {
        this.confirming = true;
    }

    /** Locks this session before any mutation or external callback. */
    public boolean beginConfirmation() {
        if (this.confirming || !hasSelection()) return false;
        this.confirming = true;
        return true;
    }

    /**
     * @return zero-based page currently displayed to the player
     */
    public int getCurrentPage() {
        return this.currentPage;
    }

    /**
     * Changes the page currently displayed to the player.
     *
     * @param currentPage zero-based destination page
     */
    public void setCurrentPage(final int currentPage) {
        this.currentPage = currentPage;
    }

    /** Returns whether at least one prize is selected. */
    public boolean hasSelection() {
        return !this.selectedPrizes.isEmpty();
    }
}
