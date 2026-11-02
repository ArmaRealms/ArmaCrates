package com.badbones69.crazycrates.tasks.crates.other;

import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.Prize;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
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

    /** Sums key costs without overflowing an integer balance. Free openings allow one choice. */
    public static long getSelectionCost(final Crate crate, final Collection<Prize> prizes, final boolean free) {
        return free ? prizes.size() : prizes.stream().mapToLong(prize -> prize.getRequiredKeys(crate.getRequiredKeys())).sum();
    }

    public boolean togglePrize(final Prize prize, final int availableKeys) {
        return togglePrize(prize, availableKeys, false);
    }

    /** Toggles a prize without allowing the total key cost to exceed the current balance. */
    public boolean togglePrize(final Prize prize, final int availableKeys, final boolean free) {
        if (this.confirming) return false;
        if (this.selectedPrizes.remove(prize)) return true;
        final long cost = getSelectionCost(this.crate, this.selectedPrizes, free)
                + (free ? 1 : prize.getRequiredKeys(this.crate.getRequiredKeys()));
        return cost <= availableKeys && (!free || this.selectedPrizes.isEmpty()) && this.selectedPrizes.add(prize);
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
