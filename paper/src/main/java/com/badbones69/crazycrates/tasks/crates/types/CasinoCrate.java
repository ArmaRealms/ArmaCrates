package com.badbones69.crazycrates.tasks.crates.types;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.builders.CrateBuilder;
import com.badbones69.crazycrates.api.enums.PersistentKeys;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.Prize;
import com.badbones69.crazycrates.api.utils.MsgUtils;
import com.badbones69.crazycrates.tasks.PrizeManager;
import com.badbones69.crazycrates.api.utils.MiscUtils;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import us.crazycrew.crazycrates.api.enums.types.KeyType;

public class CasinoCrate extends CrateBuilder {

    public CasinoCrate(Crate crate, Player player, int size) {
        super(crate, player, size);

        runTaskTimer(CrazyCrates.get(), 1, 1);
    }

    private int counter = 0;
    private int time = 1;
    private int open = 0;

    @Override
    public void run() {
        // If cancelled, we return.
        if (this.isCancelled) {
            return;
        }

        if (counter <= 50) { // When the crate is currently spinning.
            playSound("cycle-sound", SoundCategory.PLAYERS, "BLOCK_NOTE_BLOCK_XYLOPHONE");

            cycle();
        }

        open++;

        if (open >= 5) {
            getPlayer().openInventory(getInventory());
            open = 0;
        }

        counter++;

        if (counter > 51) {
            if (MiscUtils.slowSpin(120, 15).contains(time)) {
                playSound("cycle-sound", SoundCategory.PLAYERS, "BLOCK_NOTE_BLOCK_XYLOPHONE");

                cycle();
            }

            time++;

            if (time >= 60) { // When the crate task is finished.
                playSound("stop-sound", SoundCategory.PLAYERS, "ENTITY_PLAYER_LEVELUP");

                plugin.getCrateManager().endCrate(getPlayer());

                setTier(getPrize(11));
                setTier(getPrize(13));
                setTier(getPrize(15));

                plugin.getCrateManager().removePlayerFromOpeningList(getPlayer());

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (getPlayer().getOpenInventory().getTopInventory().equals(getInventory())) getPlayer().closeInventory();
                    }
                }.runTaskLater(plugin, 40);

                cancel();

                return;
            }
        }

        counter++;
    }

    private Prize setTier(Prize prize) {
        ItemStack itemStack = prize.getDisplayItem();
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        container.set(PersistentKeys.crate_tier.getNamespacedKey(), PersistentDataType.STRING, plugin.getCrazyHandler().getPrizeManager().getTier(getCrate()).getName());
        itemStack.setItemMeta(itemMeta);
        prize.setDisplayItemStack(itemStack);

        //this.plugin.getCrazyHandler().getPrizeManager().getPrize(getPlayer(), getCrate(), tier, prize);

        return prize;
    }

    @Override
    public void open(KeyType type, boolean checkHand) {
        // Crate event failed so we return.
        if (isCrateEventValid(type, checkHand)) {
            return;
        }

        boolean keyCheck = this.plugin.getCrazyHandler().getUserManager().takeKeys(1, getPlayer().getUniqueId(), getCrate().getName(), type, checkHand);

        if (!keyCheck) {
            // Send the message about failing to take the key.
            MiscUtils.failedToTakeKey(getPlayer(), getCrate());

            // Remove from opening list.
            this.plugin.getCrateManager().removePlayerFromOpeningList(getPlayer());

            return;
        }

        setDisplayItems();

        getPlayer().openInventory(getInventory());
    }

    private void setDisplayItems() {
        for (int index = 0; index <= 26; index++) {
            if (index == 2 || index == 4 || index == 6 || index == 11 || index == 13 || index == 15 || index == 20 || index == 22 || index == 24) {
                setItem(index, getDisplayItem());
            } else {
                setItem(index, getRandomGlassPane());
            }
        }
    }

    private void cycle() {
        for (int index = 0; index < 27; index++) {
            ItemStack itemStack = getInventory().getItem(index);

            if (itemStack != null) {
                ItemMeta itemMeta = itemStack.getItemMeta();

                PersistentDataContainer container = itemMeta.getPersistentDataContainer();

                if (!container.has(PersistentKeys.crate_prize.getNamespacedKey())) {
                    setItem(index, getRandomGlassPane());
                }
            }
        }

        setDisplayItems();
    }
}