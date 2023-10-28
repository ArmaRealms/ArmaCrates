package com.badbones69.crazycrates.paper.listeners;

import com.badbones69.crazycrates.paper.CrazyCrates;
import com.badbones69.crazycrates.paper.Methods;
import com.badbones69.crazycrates.paper.api.CrazyManager;
import com.badbones69.crazycrates.paper.api.FileManager.Files;
import com.badbones69.crazycrates.api.enums.Permissions;
import com.badbones69.crazycrates.paper.api.enums.settings.Messages;
import com.badbones69.crazycrates.paper.api.events.PhysicalCrateKeyCheckEvent;
import com.badbones69.crazycrates.paper.api.objects.Crate;
import com.badbones69.crazycrates.paper.api.objects.CrateLocation;
import com.badbones69.crazycrates.paper.cratetypes.QuickCrate;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import us.crazycrew.crazycrates.api.enums.types.CrateType;
import us.crazycrew.crazycrates.api.enums.types.KeyType;
import java.util.HashMap;

public class CrateControlListener implements Listener { // Crate Control
    
    // A list of crate locations that are in use.
    public static HashMap<Player, Location> inUse = new HashMap<>();

    @NotNull
    private final CrazyCrates plugin = JavaPlugin.getPlugin(CrazyCrates.class);

    @NotNull
    private final Methods methods = this.plugin.getCrazyHandler().getMethods();

    @NotNull
    private final CrazyManager crazyManager = plugin.getStarter().getCrazyManager();
    
    // This event controls when a player tries to click in a GUI based crate type. This will stop them from taking items out of their inventories.
    @EventHandler
    public void onCrateInventoryClick(InventoryClickEvent event) {
        for (Crate crate : this.crazyManager.getCrates()) {
            if (crate.getCrateType() != CrateType.menu && crate.isCrateMenu(event.getView())) event.setCancelled(true);
        }
    }
    
    // This must run as highest, so it doesn't cause other plugins to check
    // the items that were added to the players inventory and replaced the item in the player's hand.
    // This is only an issue with QuickCrate
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCrateOpen(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        FileConfiguration config = Files.CONFIG.getFile();

        if (e.getHand() == EquipmentSlot.OFF_HAND) {
            if (this.crazyManager.isKey(player.getInventory().getItemInOffHand())) {
                e.setCancelled(true);
                player.updateInventory();
            }

            return;
        }

        Block clickedBlock = e.getClickedBlock();

        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Loops through all loaded physical locations.
            for (CrateLocation loc : this.crazyManager.getCrateLocations()) {
                // Checks to see if the clicked block is the same as a physical crate.
                if (loc.getLocation().equals(clickedBlock.getLocation())) {
                    // Checks to see if the player is removing a crate location.
                    if (player.getGameMode() == GameMode.CREATIVE && player.isSneaking() && player.hasPermission("crazycrates.admin")) {
                        e.setCancelled(true);
                        this.crazyManager.removeCrateLocation(loc.getID());
                        player.sendMessage(Messages.REMOVED_PHYSICAL_CRATE.getMessage("%ID%", loc.getID()));
                        return;
                    }

                    e.setCancelled(true);

                    if (loc.getCrateType() != CrateType.menu) {
                        if (loc.getCrate().isPreviewEnabled()) {
                            PreviewListener.setPlayerInMenu(player, false);
                            PreviewListener.openNewPreview(player, loc.getCrate());
                        } else {
                            player.sendMessage(Messages.PREVIEW_DISABLED.getMessage());
                        }
                    }
                }
            }
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Checks if the item in their hand is a key and if so it stops them from right-clicking with it.
            ItemStack key = player.getInventory().getItemInMainHand();
            boolean keyInHand = this.crazyManager.isKey(key);

            if (!keyInHand) keyInHand = this.crazyManager.isKey(player.getEquipment().getItemInOffHand());

            if (keyInHand) {
                e.setCancelled(true);
                player.updateInventory();
            }

            //Checks to see if the clicked block is a physical crate.
            CrateLocation crateLocation = this.crazyManager.getCrateLocation(clickedBlock.getLocation());

            if (crateLocation != null && crateLocation.getCrate() != null) {
                Crate crate = crateLocation.getCrate();
                e.setCancelled(true);

                if (crate.getCrateType() == CrateType.menu) {
                    boolean openMenu = config.getBoolean("Settings.Enable-Crate-Menu");

                    //This is to stop players in QuadCrate to not be able to try and open a crate set to menu.
                    if (!this.crazyManager.isInOpeningList(player) && openMenu) MenuListener.openGUI(player);

                    return;
                }

                PhysicalCrateKeyCheckEvent event = new PhysicalCrateKeyCheckEvent(player, crateLocation);
                player.getServer().getPluginManager().callEvent(event);

                if (!event.isCancelled()) {
                    boolean hasKey = false;
                    boolean isPhysical = false;
                    boolean useQuickCrateAgain = false;
                    String keyName = crate.getKey().getItemMeta().getDisplayName();
                    keyName = keyName != null ? keyName : crate.getKey().getType().toString();

                    int requiredKeys = this.plugin.getCrateManager().getCrateFromName(crate.getName()).getRequiredKeys();

                    int totalKeys = this.plugin.getCrazyHandler().getUserManager().getTotalKeys(player.getUniqueId(), crate.getName());

                    if (requiredKeys > 0 && totalKeys < requiredKeys) {
                        player.sendMessage(Messages.REQUIRED_KEYS.getMessage().replaceAll("%key-amount%", String.valueOf(requiredKeys)).replaceAll("%crate%", crate.getPreviewName()).replaceAll("%amount%", String.valueOf(totalKeys)));
                        return;
                    }

                    if (crate.getCrateType() != CrateType.crate_on_the_go && keyInHand && this.crazyManager.isKeyFromCrate(key, crate) && config.getBoolean("Settings.Physical-Accepts-Physical-Keys")) {
                        hasKey = true;
                        isPhysical = true;
                    }

                    if (config.getBoolean("Settings.Physical-Accepts-Virtual-Keys") && this.plugin.getCrazyHandler().getUserManager().getVirtualKeys(player.getUniqueId(), crate.getName()) >= 1) hasKey = true;

                    if (hasKey) {
                        // Checks if the player uses the quick crate again.
                        if (this.crazyManager.isInOpeningList(player) && this.crazyManager.getOpeningCrate(player).getCrateType() == CrateType.quick_crate && inUse.containsKey(player) && inUse.get(player).equals(crateLocation.getLocation())) {
                            useQuickCrateAgain = true;
                        }

                        if (!useQuickCrateAgain) {
                            if (this.crazyManager.isInOpeningList(player)) {
                                player.sendMessage(Messages.ALREADY_OPENING_CRATE.getMessage("%Key%", keyName));
                                return;
                            }

                            if (inUse.containsValue(crateLocation.getLocation())) {
                                player.sendMessage(Messages.QUICK_CRATE_IN_USE.getMessage());
                                return;
                            }
                        }

                        if (this.methods.isInventoryFull(player)) {
                            player.sendMessage(Messages.INVENTORY_FULL.getMessage());
                            return;
                        }

                        if (useQuickCrateAgain) QuickCrate.endQuickCrate(player, crateLocation.getLocation(), crate, this.crazyManager.getHologramController(), true);

                        KeyType keyType = isPhysical ? KeyType.physical_key : KeyType.virtual_key;

                        // Only cosmic crate type uses this method.
                        if (crate.getCrateType() == CrateType.cosmic) this.crazyManager.addPlayerKeyType(player, keyType);

                        this.crazyManager.addPlayerToOpeningList(player, crate);
                        this.crazyManager.openCrate(player, crate, keyType, crateLocation.getLocation(), false, true);
                    } else {
                        if (crate.getCrateType() != CrateType.crate_on_the_go) {
                            if (config.getBoolean("Settings.KnockBack")) knockBack(player, clickedBlock.getLocation());

                            if (config.contains("Settings.Need-Key-Sound")) {
                                Sound sound = Sound.valueOf(config.getString("Settings.Need-Key-Sound"));

                                if (sound != null) player.playSound(player.getLocation(), sound, 1f, 1f);
                            }

                            player.sendMessage(Messages.NO_KEY.getMessage("%Key%", keyName));
                        }
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onAdminMenuClick(InventoryClickEvent e) {
        Inventory inv = e.getInventory();
        Player player = (Player) e.getWhoClicked();

        if (inv != null && e.getView().getTitle().equals(this.methods.sanitizeColor("&4&lAdmin Keys"))) {
            e.setCancelled(true);

            if (!this.methods.permCheck(player, Permissions.CRAZY_CRATES_ADMIN_ACCESS, false)) {
                player.closeInventory();
                return;
            }

            // Added the >= due to an error about a raw slot set at -999.
            if (e.getRawSlot() < inv.getSize() && e.getRawSlot() >= 0) { // Clicked in the admin menu.
                ItemStack item = inv.getItem(e.getRawSlot());
                if (crazyManager.isKey(item)) {
                    Crate crate = crazyManager.getCrateFromKey(item);

                    if (e.getAction() == InventoryAction.PICKUP_ALL) {
                        player.getInventory().addItem(crate.getKey());
                    } else if (e.getAction() == InventoryAction.PICKUP_HALF) {
                        this.plugin.getCrazyHandler().getUserManager().addKeys(1, player.getUniqueId(), crate.getName(), KeyType.virtual_key);
                        String name = null;
                        ItemStack key = crate.getKey();

                        if (key.hasItemMeta() && key.getItemMeta().hasDisplayName()) name = key.getItemMeta().getDisplayName();

                        player.sendMessage(this.methods.getPrefix() + this.methods.color("&a&l+1 " + (name != null ? name : crate.getName())));
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        if (this.crazyManager.hasCrateTask(player)) this.crazyManager.endCrate(player);

        if (this.crazyManager.hasQuadCrateTask(player)) this.crazyManager.endQuadCrate(player);

        if (this.crazyManager.isInOpeningList(player)) this.crazyManager.removePlayerFromOpeningList(player);
    }
    
    private void knockBack(Player player, Location location) {
        Vector vector = player.getLocation().toVector().subtract(location.toVector()).normalize().multiply(1).setY(.1);

        if (player.isInsideVehicle() && player.getVehicle() != null) {
            player.getVehicle().setVelocity(vector);
            return;
        }

        player.setVelocity(vector);
    }
}