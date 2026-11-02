package com.badbones69.crazycrates.api.builders.types;

import com.badbones69.crazycrates.api.builders.InventoryBuilder;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.Prize;
import org.bukkit.entity.Player;

import java.util.List;

public class CratePrizeMenu extends InventoryBuilder {

    private final List<Prize> displayedPrizes;

    public CratePrizeMenu(Crate crate, Player player, int size, String title) {
        super(crate, player, size, title);
        this.displayedPrizes = List.copyOf(crate.getPrizes());
    }

    /** Rejects stale slots after prizes are replaced or reordered while this menu is open. */
    public boolean hasCurrentPrizes() {
        return this.displayedPrizes.equals(getCrate().getPrizes());
    }

    @Override
    public InventoryBuilder build() {
        return this;
    }
}