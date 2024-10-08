package com.badbones69.crazycrates.api.builders.types;

import com.badbones69.crazycrates.api.builders.InventoryBuilder;
import com.badbones69.crazycrates.api.objects.Crate;
import org.bukkit.entity.Player;

public class CratePrizeMenu extends InventoryBuilder {

    public CratePrizeMenu(Crate crate, Player player, int size, String title) {
        super(crate, player, size, title);
    }

    @Override
    public InventoryBuilder build() {
        return this;
    }
}