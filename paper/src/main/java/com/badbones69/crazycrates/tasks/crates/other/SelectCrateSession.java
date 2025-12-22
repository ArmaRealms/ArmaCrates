package com.badbones69.crazycrates.tasks.crates.other;

import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.Prize;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents a player's session when opening a SelectCrate.
 * Tracks the selected prize and crate information.
 */
public class SelectCrateSession {

    private final UUID playerUUID;
    private final Crate crate;
    private Prize selectedPrize;
    private int selectedSlot;

    /**
     * Creates a new SelectCrate session for a player.
     *
     * @param player the player opening the crate
     * @param crate  the crate being opened
     */
    public SelectCrateSession(@NotNull Player player, @NotNull Crate crate) {
        this.playerUUID = player.getUniqueId();
        this.crate = crate;
        this.selectedPrize = null;
        this.selectedSlot = -1;
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

    /**
     * @return the currently selected prize, or null if none selected
     */
    @Nullable
    public Prize getSelectedPrize() {
        return this.selectedPrize;
    }

    /**
     * Sets the selected prize.
     *
     * @param prize the prize to select
     * @param slot  the slot of the selected prize
     */
    public void setSelectedPrize(@Nullable Prize prize, int slot) {
        this.selectedPrize = prize;
        this.selectedSlot = slot;
    }

    /**
     * @return the slot of the selected prize, or -1 if none selected
     */
    public int getSelectedSlot() {
        return this.selectedSlot;
    }

    /**
     * @return true if a prize is currently selected
     */
    public boolean hasSelection() {
        return this.selectedPrize != null;
    }

    /**
     * Clears the current selection.
     */
    public void clearSelection() {
        this.selectedPrize = null;
        this.selectedSlot = -1;
    }
}
