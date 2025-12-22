package com.badbones69.crazycrates.tasks.crates.types;

import com.badbones69.crazycrates.api.builders.CrateBuilder;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.Prize;
import com.badbones69.crazycrates.api.objects.other.ItemBuilder;
import com.badbones69.crazycrates.api.utils.MsgUtils;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import us.crazycrew.crazycrates.api.enums.types.KeyType;

import java.util.ArrayList;
import java.util.List;

/**
 * SelectCrate allows players to choose which prize they want to receive.
 * Players see all available prizes in a GUI and select one before confirming.
 */
public class SelectCrate extends CrateBuilder {

    /**
     * Creates a new SelectCrate instance.
     *
     * @param crate  the crate being opened
     * @param player the player opening the crate
     * @param size   the inventory size
     */
    public SelectCrate(final Crate crate, final Player player, final int size) {
        super(crate, player, size, getGuiTitle(crate));
    }

    /**
     * Gets the GUI title from config or returns default.
     *
     * @param crate the crate
     * @return the GUI title
     */
    private static String getGuiTitle(final Crate crate) {
        final FileConfiguration file = crate.getFile();
        return file != null ?
                MsgUtils.color(file.getString("Crate.SelectCrate.GUI.Title", "&6Select Your Prize!")) :
                "&6Select Your Prize!";
    }

    @Override
    public void open(final KeyType type, final boolean checkHand) {
        // If the crate event failed, return
        if (isCrateEventValid(type, checkHand)) {
            return;
        }

        final FileConfiguration file = getCrate().getFile();

        // Add prizes to the inventory
        final List<Prize> prizes = getCrate().getPrizes();
        int slot = 0;

        for (final Prize prize : prizes) {
            if (slot >= getSize() - 9) { // Reserve bottom row for controls
                break;
            }

            final ItemStack displayItem = prize.getDisplayItem(getPlayer());
            setItem(slot, displayItem);
            slot++;
        }

        // Add confirm button
        final int confirmSlot = file != null ?
                file.getInt("Crate.SelectCrate.Confirm.Slot", 49) : 49;

        final ItemBuilder confirmButton = getConfirmButton(file);
        setItem(confirmSlot, confirmButton.build());

        // Store the key type and hand check for later use
        this.plugin.getCrateManager().addPlayerKeyType(getPlayer(), type);
        this.plugin.getCrateManager().addHands(getPlayer(), checkHand);

        // Open the inventory
        getPlayer().openInventory(getInventory());
    }

    @Override
    public void run() {
        // No animation for SelectCrate
    }

    /**
     * Gets the confirm button from config or creates a default one.
     *
     * @param file the crate configuration file
     * @return the confirm button ItemBuilder
     */
    private ItemBuilder getConfirmButton(final FileConfiguration file) {
        final ItemBuilder builder = new ItemBuilder();

        if (file != null && file.contains("Crate.SelectCrate.Confirm.Item")) {
            final String material = file.getString("Crate.SelectCrate.Confirm.Item.Material", "LIME_CONCRETE");
            final String name = file.getString("Crate.SelectCrate.Confirm.Item.Name", "&aConfirmar escolha");
            List<String> lore = file.getStringList("Crate.SelectCrate.Confirm.Item.Lore");

            if (lore.isEmpty()) {
                lore = new ArrayList<>();
                lore.add("&7Click to receive the selected prize.");
            }

            builder.setMaterial(material)
                    .setName(MsgUtils.color(name))
                    .setLore(lore);
        } else {
            // Default confirm button
            builder.setMaterial(Material.LIME_CONCRETE)
                    .setName(MsgUtils.color("&aConfirm Choice"))
                    .setLore(List.of(MsgUtils.color("&7Click to receive the selected prize.")));
        }

        return builder;
    }

    /**
     * Gets the selection marker item from config or creates a default one.
     *
     * @param file the crate configuration file
     * @return the selection marker ItemBuilder
     */
    public static ItemBuilder getSelectionMarker(final FileConfiguration file) {
        final ItemBuilder builder = new ItemBuilder();

        if (file != null && file.contains("Crate.SelectCrate.SelectionMarker")) {
            final String material = file.getString("Crate.SelectCrate.SelectionMarker.Material", "NETHER_STAR");
            final String name = file.getString("Crate.SelectCrate.SelectionMarker.Name", "&eSelecionado");
            List<String> lore = file.getStringList("Crate.SelectCrate.SelectionMarker.Lore");

            if (lore.isEmpty()) {
                lore = new ArrayList<>();
                lore.add("&7This prize is selected.");
            }

            builder.setMaterial(material)
                    .setName(MsgUtils.color(name))
                    .setLore(lore);
        } else {
            // Default selection marker
            builder.setMaterial(Material.NETHER_STAR)
                    .setName(MsgUtils.color("&eSelected"))
                    .setLore(List.of(MsgUtils.color("&7This prize is selected.")));
        }

        return builder;
    }
}
