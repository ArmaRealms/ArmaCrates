package com.badbones69.crazycrates.listeners.crates;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.builders.types.CrateMainMenu;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.other.CrateLocation;
import com.badbones69.crazycrates.api.utils.MiscUtils;
import com.badbones69.crazycrates.common.config.types.ConfigKeys;
import com.badbones69.crazycrates.common.config.types.MessageKeys;
import com.badbones69.crazycrates.listeners.CrateControlListener;
import com.badbones69.crazycrates.tasks.crates.CrateManager;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import us.crazycrew.crazycrates.api.enums.types.CrateType;
import us.crazycrew.crazycrates.api.enums.types.KeyType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SelectCrateOpeningCostTest {
    @Test void physicalSelectMenuCanOpenWithLessThanGlobalCost() {
        checkPhysicalOpening(false, CrateType.select_crate, 2, true);
    }

    @Test void physicalSelectMenuCanOpenWithNoEmptySlots() {
        checkPhysicalOpening(true, CrateType.select_crate, 2, true);
    }

    @Test void otherCratesKeepTheirEarlyInventoryGuard() {
        checkPhysicalOpening(true, CrateType.csgo, 1, false);
    }

    private void checkPhysicalOpening(final boolean full, final CrateType type, final int required, final boolean opens) {
        final CrazyCrates plugin = mock(CrazyCrates.class, RETURNS_DEEP_STUBS);
        final CrateManager manager = mock(CrateManager.class);
        final Player player = mock(Player.class, RETURNS_DEEP_STUBS);
        final Block block = mock(Block.class);
        final Location location = mock(Location.class);
        final Crate crate = mock(Crate.class);
        final CrateLocation placed = new CrateLocation("test", crate, location);
        final PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        try (var singleton = mockStatic(CrazyCrates.class); var misc = mockStatic(MiscUtils.class)) {
            singleton.when(CrazyCrates::get).thenReturn(plugin);
            when(plugin.getCrateManager()).thenReturn(manager);
            misc.when(() -> MiscUtils.isInventoryFull(player)).thenReturn(full);
            when(plugin.getConfigManager().getConfig().getProperty(ConfigKeys.physical_accepts_virtual_keys)).thenReturn(true);
            when(plugin.getCrazyHandler().getConfigManager().getMessages().getProperty(MessageKeys.required_keys)).thenReturn("Insufficient keys");
            when(plugin.getCrazyHandler().getConfigManager().getMessages().getProperty(MessageKeys.inventory_not_empty)).thenReturn("Inventory full");
            when(plugin.getCrazyHandler().getConfigManager().getMessages().getProperty(MessageKeys.no_keys)).thenReturn("No keys");
            when(plugin.getCrazyHandler().getConfigManager().getMessages().getProperty(MessageKeys.no_crate_permission)).thenReturn("No permission");
            when(plugin.getCrazyHandler().getConfigManager().getConfig().getProperty(ConfigKeys.command_prefix)).thenReturn("");
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(event.getPlayer()).thenReturn(player);
            when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
            when(event.getClickedBlock()).thenReturn(block);
            when(block.getLocation()).thenReturn(location);
            when(manager.getCrateLocation(location)).thenReturn(placed);
            when(manager.getCrateFromName("test")).thenReturn(crate);
            when(manager.getCratesInUse()).thenReturn(Map.of());
            when(crate.getName()).thenReturn("test");
            when(crate.getRequiredKeys()).thenReturn(required);
            when(crate.getCrateType()).thenReturn(type);
            when(plugin.getCrazyHandler().getUserManager().getTotalKeys(player.getUniqueId(), "test")).thenReturn(1);
            when(plugin.getCrazyHandler().getUserManager().getVirtualKeys(player.getUniqueId(), "test")).thenReturn(1);
            final CrateControlListener listener = new CrateControlListener();
            listener.onCrateOpen(event);
            if (opens) verify(manager).openCrate(player, crate, KeyType.virtual_key, location, false, true);
            else verify(manager, never()).openCrate(any(), any(), any(), any(), anyBoolean(), anyBoolean());
        }
    }
    @Test void mainMenuCanOpenSelectCrateWithNoEmptySlots() {
        final CrazyCrates plugin = mock(CrazyCrates.class, RETURNS_DEEP_STUBS);
        final CrateManager manager = mock(CrateManager.class);
        final Player player = mock(Player.class, RETURNS_DEEP_STUBS);
        final Crate crate = mock(Crate.class);
        final CrateMainMenu holder = mock(CrateMainMenu.class);
        final Inventory inventory = mock(Inventory.class);
        final ItemStack item = mock(ItemStack.class);
        final Material material = mock(Material.class);
        final InventoryClickEvent event = mock(InventoryClickEvent.class);
        try (var singleton = mockStatic(CrazyCrates.class);
             var misc = mockStatic(MiscUtils.class);
             var nbt = mockConstruction(NBTItem.class, (constructed, context) -> {
                 when(constructed.hasNBTData()).thenReturn(true);
                 when(constructed.getString("CrazyCrates-Crate")).thenReturn("test");
             })) {
            singleton.when(CrazyCrates::get).thenReturn(plugin);
            when(plugin.getCrateManager()).thenReturn(manager);
            when(plugin.getConfigManager().getConfig().getProperty(ConfigKeys.disabledWorlds)).thenReturn(List.of());
            when(plugin.getCrazyHandler().getConfigManager().getMessages().getProperty(MessageKeys.inventory_not_empty)).thenReturn("Inventory full");
            when(plugin.getCrazyHandler().getConfigManager().getMessages().getProperty(MessageKeys.no_keys)).thenReturn("No keys");
            when(plugin.getCrazyHandler().getConfigManager().getMessages().getProperty(MessageKeys.no_crate_permission)).thenReturn("No permission");
            when(plugin.getCrazyHandler().getConfigManager().getConfig().getProperty(ConfigKeys.command_prefix)).thenReturn("");
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(holder.getPlayer()).thenReturn(player);
            when(inventory.getHolder()).thenReturn(holder);
            when(inventory.getHolder(false)).thenReturn(holder);
            when(event.getInventory()).thenReturn(inventory);
            when(event.getCurrentItem()).thenReturn(item);
            when(item.getType()).thenReturn(material);
            when(item.hasItemMeta()).thenReturn(true);
            when(crate.getName()).thenReturn("test");
            when(crate.getCrateType()).thenReturn(CrateType.select_crate);
            when(manager.getCrateFromName("test")).thenReturn(crate);
            when(plugin.getCrazyHandler().getUserManager().getVirtualKeys(player.getUniqueId(), "test")).thenReturn(1);
            misc.when(() -> MiscUtils.isInventoryFull(player)).thenReturn(true);
            final Location location = player.getLocation();
            new CrateMainMenu.CrateMenuListener().onInventoryClick(event);
            verify(manager).openCrate(player, crate, KeyType.virtual_key, location, true, false);
        }
    }

}
