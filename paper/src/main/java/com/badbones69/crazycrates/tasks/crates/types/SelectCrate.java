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
    public SelectCrate(Crate crate, Player player, int size) {
        super(crate, player, size);
    }

    @Override
    public void open(KeyType type, boolean checkHand) {
        // If the crate event failed, return
        if (isCrateEventValid(type, checkHand)) {
            return;
        }

        FileConfiguration file = getCrate().getFile();
        
        // Get inventory title from config or use default
        String title = file != null ? 
            MsgUtils.color(file.getString("Crate.SelectCrate.GUI.Title", "&6Selecione seu prêmio!")) :
            "&6Selecione seu prêmio!";
        
        setTitle(title);

        // Add prizes to the inventory
        List<Prize> prizes = getCrate().getPrizes();
        int slot = 0;
        
        for (Prize prize : prizes) {
            if (slot >= getSize() - 9) { // Reserve bottom row for controls
                break;
            }
            
            ItemStack displayItem = prize.getDisplayItem(getPlayer());
            setItem(slot, displayItem);
            slot++;
        }

        // Add confirm button
        int confirmSlot = file != null ? 
            file.getInt("Crate.SelectCrate.Confirm.Slot", 49) : 49;
        
        ItemBuilder confirmButton = getConfirmButton(file);
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
    private ItemBuilder getConfirmButton(FileConfiguration file) {
        ItemBuilder builder = new ItemBuilder();
        
        if (file != null && file.contains("Crate.SelectCrate.Confirm.Item")) {
            String material = file.getString("Crate.SelectCrate.Confirm.Item.Material", "LIME_CONCRETE");
            String name = file.getString("Crate.SelectCrate.Confirm.Item.Name", "&aConfirmar escolha");
            List<String> lore = file.getStringList("Crate.SelectCrate.Confirm.Item.Lore");
            
            if (lore.isEmpty()) {
                lore = new ArrayList<>();
                lore.add("&7Clique para receber o prêmio selecionado.");
            }
            
            builder.setMaterial(material)
                   .setName(MsgUtils.color(name))
                   .setLore(lore);
        } else {
            // Default confirm button
            builder.setMaterial(Material.LIME_CONCRETE)
                   .setName(MsgUtils.color("&aConfirmar escolha"))
                   .setLore(List.of(MsgUtils.color("&7Clique para receber o prêmio selecionado.")));
        }
        
        return builder;
    }

    /**
     * Gets the selection marker item from config or creates a default one.
     *
     * @param file the crate configuration file
     * @return the selection marker ItemBuilder
     */
    public static ItemBuilder getSelectionMarker(FileConfiguration file) {
        ItemBuilder builder = new ItemBuilder();
        
        if (file != null && file.contains("Crate.SelectCrate.SelectionMarker")) {
            String material = file.getString("Crate.SelectCrate.SelectionMarker.Material", "NETHER_STAR");
            String name = file.getString("Crate.SelectCrate.SelectionMarker.Name", "&eSelecionado");
            List<String> lore = file.getStringList("Crate.SelectCrate.SelectionMarker.Lore");
            
            if (lore.isEmpty()) {
                lore = new ArrayList<>();
                lore.add("&7Este prêmio está selecionado.");
            }
            
            builder.setMaterial(material)
                   .setName(MsgUtils.color(name))
                   .setLore(lore);
        } else {
            // Default selection marker
            builder.setMaterial(Material.NETHER_STAR)
                   .setName(MsgUtils.color("&eSelecionado"))
                   .setLore(List.of(MsgUtils.color("&7Este prêmio está selecionado.")));
        }
        
        return builder;
    }
}
