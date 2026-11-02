package com.badbones69.crazycrates.listeners.crates;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.PrizeManager;
import com.badbones69.crazycrates.api.events.PlayerPrizeEvent;
import com.badbones69.crazycrates.api.builders.types.CratePrizeMenu;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.Prize;
import com.badbones69.crazycrates.api.utils.ItemUtils;
import com.badbones69.crazycrates.api.utils.MiscUtils;
import com.badbones69.crazycrates.common.config.types.ConfigKeys;
import com.badbones69.crazycrates.common.config.types.MessageKeys;
import com.badbones69.crazycrates.tasks.BukkitUserManager;
import com.badbones69.crazycrates.tasks.crates.CrateManager;
import com.badbones69.crazycrates.tasks.crates.types.SelectCrate;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.ArgumentCaptor;
import us.crazycrew.crazycrates.api.enums.types.CrateType;
import us.crazycrew.crazycrates.api.enums.types.KeyType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SelectCrateListenerTest {
    private final CrazyCrates plugin = mock(CrazyCrates.class, RETURNS_DEEP_STUBS);
    private final CrateManager manager = mock(CrateManager.class);
    private final BukkitUserManager users = mock(BukkitUserManager.class);
    private final Player player = mock(Player.class);
    private final PlayerInventory inventory = mock(PlayerInventory.class);
    private final Inventory top = mock(Inventory.class);
    private final InventoryView view = mock(InventoryView.class);
    private final Crate crate = mock(Crate.class);
    private final List<Prize> prizes = new ArrayList<>();
    private final List<Runnable> tasks = new ArrayList<>();
    private MockedStatic<CrazyCrates> singleton;
    private MockedStatic<SelectCrate> select;
    private MockedStatic<PrizeManager> delivery;
    private MockedStatic<MiscUtils> misc;
    private MockedStatic<ItemUtils> itemUtils;
    private SelectCrateListener listener;
    private ItemStack key;

    private ItemStack item() {
        final ItemStack item = mock(ItemStack.class);
        final Material material = mock(Material.class);
        when(item.getType()).thenReturn(material);
        when(item.getAmount()).thenReturn(1);
        when(item.getMaxStackSize()).thenReturn(64);
        when(item.clone()).thenReturn(item);
        return item;
    }

    @BeforeEach void setup() {
        singleton = mockStatic(CrazyCrates.class);
        singleton.when(CrazyCrates::get).thenReturn(plugin);
        when(plugin.getCrateManager()).thenReturn(manager);
        when(plugin.getUserManager()).thenReturn(users);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getCrazyHandler().getConfigManager().getMessages().getProperty(MessageKeys.no_keys)).thenReturn("No keys");
        when(plugin.getCrazyHandler().getConfigManager().getMessages().getProperty(MessageKeys.no_crate_permission)).thenReturn("No permission");
        when(plugin.getCrazyHandler().getConfigManager().getConfig().getProperty(ConfigKeys.command_prefix)).thenReturn("");
        select = mockStatic(SelectCrate.class, CALLS_REAL_METHODS);
        select.when(() -> SelectCrate.renderPage(any(), any(), any(), anyInt(), anySet(), anyInt(), anyBoolean())).thenAnswer(call -> null);
        delivery = mockStatic(PrizeManager.class);
        misc = mockStatic(MiscUtils.class);
        itemUtils = mockStatic(ItemUtils.class);
        key = item();
        itemUtils.when(() -> ItemUtils.isSimilar(any(), same(crate))).thenAnswer(call -> call.getArgument(0) == key);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getInventory()).thenReturn(inventory);
        when(player.hasPermission(anyString())).thenReturn(true);
        when(player.getOpenInventory()).thenReturn(view);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[36]);
        final ItemStack offhand = item();
        when(inventory.getItemInOffHand()).thenReturn(offhand);
        when(inventory.getMaxStackSize()).thenReturn(64);
        when(view.getTopInventory()).thenReturn(top);
        when(top.getSize()).thenReturn(54);
        when(top.getItem(anyInt())).thenAnswer(call -> item());
        when(crate.getCrateType()).thenReturn(CrateType.select_crate);
        when(crate.getName()).thenReturn("select");
        when(crate.getRequiredKeys()).thenReturn(1);
        for (int i = 0; i < 6; i++) {
            final Prize prize = mock(Prize.class);
            when(prize.getRequiredKeys(anyInt())).thenAnswer(call -> Math.max(1, (int) call.getArgument(0)));
            final ItemStack reward = item();
            when(prize.getItems()).thenReturn(List.of(reward));
            prizes.add(prize);
        }
        when(crate.getPrizes()).thenReturn(prizes);
        final CratePrizeMenu holder = new CratePrizeMenu(crate, player, 54, "select");
        when(top.getHolder(false)).thenReturn(holder);
        when(manager.getOpeningCrate(player)).thenReturn(crate);
        when(manager.getCrateFromName("select")).thenReturn(crate);
        when(manager.isInOpeningList(player)).thenReturn(true);
        when(manager.getPlayerKeyType(player)).thenReturn(KeyType.virtual_key);
        when(users.getVirtualKeys(player.getUniqueId(), "select")).thenReturn(5);
        when(users.takeKeys(anyInt(), any(), anyString(), any(), anyBoolean())).thenReturn(true);
        final var scheduler = plugin.getServer().getScheduler();
        doAnswer(call -> { tasks.add(call.getArgument(1)); return null; })
                .when(scheduler).runTask(eq(plugin), any(Runnable.class));
        listener = new SelectCrateListener();
    }

    @AfterEach void teardown() {
        itemUtils.close();
        misc.close();
        delivery.close();
        select.close();
        singleton.close();
    }

    private void click(final int slot) {
        final InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(top);
        when(event.getClickedInventory()).thenReturn(top);
        when(event.getView()).thenReturn(view);
        when(event.getRawSlot()).thenReturn(slot);
        listener.onInventoryClick(event);
        verify(event).setCancelled(true);
    }

    private void tick() {
        final List<Runnable> pending = List.copyOf(tasks);
        tasks.clear();
        pending.forEach(Runnable::run);
    }

    @Test void repeatedConfirmClicksDeliverAndDebitExactlyOnce() {
        click(0);
        click(1);
        click(49);
        click(49);
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        tick();
        verify(users).takeKeys(2, player.getUniqueId(), "select", KeyType.virtual_key, false);
        verify(inventory).setStorageContents(argThat(contents -> contents[0] != null && contents[1] != null));
        delivery.verify(() -> PrizeManager.finishPrize(player, prizes.get(0), crate));
        delivery.verify(() -> PrizeManager.finishPrize(player, prizes.get(1), crate));
        delivery.verifyNoMoreInteractions();
    }

    @Test void insufficientInventoryCancelsWholeBatch() {
        final ItemStack[] full = new ItemStack[36];
        java.util.Arrays.fill(full, item());
        when(inventory.getStorageContents()).thenReturn(full);
        click(0);
        click(1);
        click(49);
        tick();
        verify(player).sendMessage(contains("Você não possui espaço suficiente no inventário para receber todos os produtos selecionados."));
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        verify(inventory, never()).setStorageContents(any());
        delivery.verifyNoInteractions();
    }

    @Test void cancelNeverConsumesKeysOrDeliversRewards() {
        click(0);
        click(53);
        tick();
        verify(player).closeInventory(InventoryCloseEvent.Reason.PLUGIN);
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        verify(inventory, never()).setStorageContents(any());
        delivery.verifyNoInteractions();
    }

    @Test void closingBeforeQueuedConfirmationCancelsIt() {
        click(0);
        click(49);
        final InventoryCloseEvent close = mock(InventoryCloseEvent.class);
        when(close.getPlayer()).thenReturn(player);
        when(close.getInventory()).thenReturn(top);
        listener.onInventoryClose(close);
        tick();
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        delivery.verifyNoInteractions();
    }

    @Test void reducedVirtualBalanceCannotRedeemOldSelection() {
        click(0);
        click(1);
        when(users.getVirtualKeys(player.getUniqueId(), "select")).thenReturn(1);
        click(49);
        tick();
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        delivery.verifyNoInteractions();
    }

    @Test void requiredKeysAreChargedPerSelection() {
        when(crate.getRequiredKeys()).thenReturn(2);
        click(0);
        click(1);
        click(2);
        click(49);
        tick();
        verify(users).takeKeys(4, player.getUniqueId(), "select", KeyType.virtual_key, false);
        delivery.verify(() -> PrizeManager.finishPrize(player, prizes.get(2), crate), never());
    }

    @Test void failedInventoryCommitRestoresInventoryAndVirtualKeys() {
        click(0);
        doThrow(new IllegalStateException("commit failed")).doNothing().when(inventory).setStorageContents(any());
        click(49);
        tick();
        verify(users).setKeys(5, player.getUniqueId(), "select");
        verify(inventory, times(2)).setStorageContents(any());
        delivery.verifyNoInteractions();
    }

    @Test void staleCrateAfterReloadCannotRedeem() {
        click(0);
        when(manager.getCrateFromName("select")).thenReturn(mock(Crate.class));
        click(49);
        tick();
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        delivery.verifyNoInteractions();
    }
    @Test void cancelClickInvalidatesConfirmationAlreadyQueued() {
        click(0);
        click(49);
        click(53);
        tick();
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        verify(inventory, never()).setStorageContents(any());
        delivery.verifyNoInteractions();
    }

    @Test void freeOpeningAllowsOnlyOneRewardWithoutDebit() {
        when(manager.getPlayerKeyType(player)).thenReturn(KeyType.free_key);
        click(0);
        click(1);
        click(49);
        tick();
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        delivery.verify(() -> PrizeManager.finishPrize(player, prizes.get(0), crate));
        delivery.verifyNoMoreInteractions();
    }

    @Test void physicalKeyIsDebitedInTheSameInventoryCandidateAsTheReward() {
        when(manager.getPlayerKeyType(player)).thenReturn(KeyType.physical_key);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{key});
        click(0);
        click(49);
        tick();
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        verify(inventory).setStorageContents(argThat(contents -> contents[0] == prizes.get(0).getItems().getFirst()));
        delivery.verify(() -> PrizeManager.finishPrize(player, prizes.get(0), crate));
    }

    @Test void physicalKeysRemainUntouchedWhenRewardsDoNotFit() {
        when(manager.getPlayerKeyType(player)).thenReturn(KeyType.physical_key);
        when(inventory.getItemInOffHand()).thenReturn(key);
        final ItemStack occupied = item();
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{occupied});
        click(0);
        click(49);
        tick();
        verify(inventory, never()).setStorageContents(any());
        verify(inventory, never()).setItemInOffHand(any());
        delivery.verifyNoInteractions();
    }

    @Test void blacklistedPrizeCannotBeSelectedEvenWithAlternative() {
        final Prize alternative = mock(Prize.class);
        when(prizes.get(0).hasPermission(player)).thenReturn(true);
        when(prizes.get(0).getAlternativePrize()).thenReturn(alternative);
        click(0);
        click(49);
        tick();
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        verify(inventory, never()).setStorageContents(any());
        delivery.verifyNoInteractions();
    }

    @Test void callbackFailureDoesNotRepeatDebitOrPreventLaterRewards() {
        click(0);
        click(1);
        delivery.when(() -> PrizeManager.finishPrize(player, prizes.get(0), crate))
                .thenThrow(new IllegalStateException("external command failed"));
        click(49);
        tick();
        verify(users).takeKeys(2, player.getUniqueId(), "select", KeyType.virtual_key, false);
        delivery.verify(() -> PrizeManager.finishPrize(player, prizes.get(1), crate));
    }

    @Test void revokedCratePermissionCannotRedeemQueuedSelection() {
        click(0);
        click(49);
        when(player.hasPermission("crazycrates.open.select")).thenReturn(false);
        tick();
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        verify(inventory, never()).setStorageContents(any());
        delivery.verifyNoInteractions();
    }

    @Test void prizeChangesBeforeFirstClickInvalidateDisplayedMenu() {
        java.util.Collections.swap(prizes, 0, 1);
        click(0);
        click(49);
        tick();
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        verify(inventory, never()).setStorageContents(any());
        delivery.verifyNoInteractions();
    }

    @Test void deliveredItemsStillFirePrizeEventWhenExternalCommandsThrow() {
        click(0);
        click(1);
        delivery.when(() -> PrizeManager.finishPrize(player, prizes.get(0), crate))
                .thenThrow(new IllegalStateException("external command failed"));
        click(49);
        tick();
        final var events = ArgumentCaptor.forClass(PlayerPrizeEvent.class);
        verify(plugin.getServer().getPluginManager(), times(2)).callEvent(events.capture());
        assertEquals(List.of(prizes.get(0), prizes.get(1)),
                events.getAllValues().stream().map(PlayerPrizeEvent::getPrize).toList());
    }

    @Test void prizeChangeAfterConfirmationClickInvalidatesQueuedRedemption() {
        click(0);
        click(49);
        java.util.Collections.swap(prizes, 0, 1);
        tick();
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        verify(inventory, never()).setStorageContents(any());
        delivery.verifyNoInteractions();
    }

    @Test void failedFireworksStillFirePrizeEventForDeliveredItems() {
        when(prizes.get(0).useFireworks()).thenReturn(true);
        final Location location = mock(Location.class);
        when(player.getLocation()).thenReturn(location);
        when(location.add(0, 1, 0)).thenThrow(new IllegalStateException("firework location unavailable"));
        click(0);
        click(49);
        tick();
        final var event = ArgumentCaptor.forClass(PlayerPrizeEvent.class);
        verify(plugin.getServer().getPluginManager()).callEvent(event.capture());
        assertSame(prizes.get(0), event.getValue().getPrize());
        verify(users).takeKeys(1, player.getUniqueId(), "select", KeyType.virtual_key, false);
    }

    @Test void closingAndReopeningDuringPreparationCancelsCapturedConfirmation() {
        click(0);
        when(prizes.get(0).hasPermission(player)).thenAnswer(call -> {
            final InventoryCloseEvent close = mock(InventoryCloseEvent.class);
            when(close.getPlayer()).thenReturn(player);
            when(close.getInventory()).thenReturn(top);
            listener.onInventoryClose(close);
            // A synchronous integration can reopen the same crate before preparation returns.
            click(1);
            return false;
        });
        click(49);
        tick();
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        verify(inventory, never()).setStorageContents(any());
        delivery.verifyNoInteractions();
        click(49);
        tick();
        verify(users).takeKeys(1, player.getUniqueId(), "select", KeyType.virtual_key, false);
        delivery.verify(() -> PrizeManager.finishPrize(player, prizes.get(1), crate));
        delivery.verifyNoMoreInteractions();
    }

    @Test void failedVirtualDebitNeverDeliversItemsOrCallbacks() {
        when(users.takeKeys(anyInt(), any(), anyString(), any(), anyBoolean())).thenReturn(false);
        click(0);
        click(49);
        tick();
        verify(inventory, never()).setStorageContents(any());
        verify(player).closeInventory(InventoryCloseEvent.Reason.PLUGIN);
        delivery.verifyNoInteractions();
    }

    @Test void newlyBlacklistedPrizeCancelsWholeBatchAndCanBeReplaced() {
        click(0);
        click(1);
        when(prizes.get(0).hasPermission(player)).thenReturn(true);
        click(49);
        tick();
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        verify(inventory, never()).setStorageContents(any());
        delivery.verifyNoInteractions();
        click(2);
        click(49);
        tick();
        verify(users).takeKeys(2, player.getUniqueId(), "select", KeyType.virtual_key, false);
        delivery.verify(() -> PrizeManager.finishPrize(player, prizes.get(1), crate));
        delivery.verify(() -> PrizeManager.finishPrize(player, prizes.get(2), crate));
        delivery.verifyNoMoreInteractions();
    }

    @Test void blockedSlotDoesNotUseSelectionCapacity() {
        when(users.getVirtualKeys(player.getUniqueId(), "select")).thenReturn(1);
        when(prizes.get(0).hasPermission(player)).thenReturn(true);
        click(0);
        click(1);
        click(49);
        tick();
        verify(users).takeKeys(1, player.getUniqueId(), "select", KeyType.virtual_key, false);
        delivery.verify(() -> PrizeManager.finishPrize(player, prizes.get(1), crate));
        delivery.verifyNoMoreInteractions();
    }

    @Test void blacklistGrantedDuringPreparationRejectsBatch() {
        click(0);
        when(prizes.get(0).hasPermission(player)).thenReturn(false, true);
        click(49);
        tick();
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        verify(inventory, never()).setStorageContents(any());
        delivery.verifyNoInteractions();
    }

    @Test void newlyBlockedChoiceReleasesCapacityBeforeSelectingReplacement() {
        when(users.getVirtualKeys(player.getUniqueId(), "select")).thenReturn(1);
        click(0);
        when(prizes.get(0).hasPermission(player)).thenReturn(true);
        click(1);
        click(49);
        tick();
        verify(users).takeKeys(1, player.getUniqueId(), "select", KeyType.virtual_key, false);
        delivery.verify(() -> PrizeManager.finishPrize(player, prizes.get(1), crate));
        delivery.verifyNoMoreInteractions();
    }

    @Test void repeatedConfirmCannotRedeemRemainingChoicesAfterBlacklistRejection() {
        click(0);
        click(1);
        when(prizes.get(0).hasPermission(player)).thenReturn(true);
        click(49);
        click(49);
        tick();
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        verify(inventory, never()).setStorageContents(any());
        delivery.verifyNoInteractions();
        // The remaining valid choice needs a new, explicit confirmation.
        click(49);
        tick();
        verify(users).takeKeys(1, player.getUniqueId(), "select", KeyType.virtual_key, false);
        delivery.verify(() -> PrizeManager.finishPrize(player, prizes.get(1), crate));
        delivery.verifyNoMoreInteractions();
    }

    @Test void navigatingDropsNewlyBlockedChoicesFromCounter() {
        for (int index = prizes.size(); index < 46; index++) prizes.add(mock(Prize.class));
        final CratePrizeMenu holder = new CratePrizeMenu(crate, player, 54, "select");
        when(top.getHolder(false)).thenReturn(holder);
        when(users.getVirtualKeys(player.getUniqueId(), "select")).thenReturn(1);
        click(0);
        when(prizes.get(0).hasPermission(player)).thenReturn(true);
        click(50);
        select.verify(() -> SelectCrate.renderPage(top, player, crate, 1, java.util.Set.of(), 1, false));
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
    }

    @Test void higherPrizeCostOverridesGlobalAndMixedChoicesDebitTheirSum() {
        when(prizes.get(0).getRequiredKeys(anyInt())).thenReturn(2);
        click(0);
        click(1);
        click(49);
        tick();
        verify(users).takeKeys(3, player.getUniqueId(), "select", KeyType.virtual_key, false);
    }

    @Test void cheaperPrizeOverridesGlobalEvenWhenBalanceIsBelowGlobal() {
        when(crate.getRequiredKeys()).thenReturn(2);
        when(users.getVirtualKeys(player.getUniqueId(), "select")).thenReturn(1);
        when(prizes.get(0).getRequiredKeys(anyInt())).thenReturn(1);
        click(0);
        click(49);
        tick();
        verify(users).takeKeys(1, player.getUniqueId(), "select", KeyType.virtual_key, false);
        delivery.verify(() -> PrizeManager.finishPrize(player, prizes.get(0), crate));
    }

    @Test void individuallyExpensivePrizeCannotExceedRemainingBudget() {
        when(users.getVirtualKeys(player.getUniqueId(), "select")).thenReturn(2);
        when(prizes.get(0).getRequiredKeys(anyInt())).thenReturn(2);
        click(0);
        click(1);
        click(49);
        tick();
        verify(users).takeKeys(2, player.getUniqueId(), "select", KeyType.virtual_key, false);
        delivery.verify(() -> PrizeManager.finishPrize(player, prizes.get(0), crate));
        delivery.verifyNoMoreInteractions();
    }

    @Test void overrideCostIsRevalidatedWhenConfirmationRuns() {
        when(users.getVirtualKeys(player.getUniqueId(), "select")).thenReturn(1);
        click(0);
        click(49);
        when(prizes.get(0).getRequiredKeys(anyInt())).thenReturn(2);
        tick();
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        verify(inventory, never()).setStorageContents(any());
        delivery.verifyNoInteractions();
    }

    @Test void physicalKeysPayThePrizeOverrideExactly() {
        when(manager.getPlayerKeyType(player)).thenReturn(KeyType.physical_key);
        when(key.getAmount()).thenReturn(2);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{key});
        when(prizes.get(0).getRequiredKeys(anyInt())).thenReturn(2);
        click(0);
        click(49);
        tick();
        verify(inventory).setStorageContents(argThat(contents -> contents[0] == prizes.get(0).getItems().getFirst()));
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        delivery.verify(() -> PrizeManager.finishPrize(player, prizes.get(0), crate));
    }

    @Test void defaultCapPreventsSixthSelectionWithSufficientBalance() {
        when(users.getVirtualKeys(player.getUniqueId(), "select")).thenReturn(100);
        for (int slot = 0; slot < 6; slot++) click(slot);
        click(49);
        tick();
        verify(users).takeKeys(5, player.getUniqueId(), "select", KeyType.virtual_key, false);
        delivery.verify(() -> PrizeManager.finishPrize(player, prizes.get(5), crate), never());
    }

    @Test void capReducedBeforeConfirmationRejectsBatchUntilPlayerDeselects() {
        final YamlConfiguration config = new YamlConfiguration();
        when(crate.getFile()).thenReturn(config);
        click(0);
        click(1);
        click(49);
        config.set("Crate.SelectCrate.MaxSelections", 1);
        tick();
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        delivery.verifyNoInteractions();
        click(0);
        click(49);
        tick();
        verify(users).takeKeys(1, player.getUniqueId(), "select", KeyType.virtual_key, false);
        delivery.verify(() -> PrizeManager.finishPrize(player, prizes.get(1), crate));
        delivery.verifyNoMoreInteractions();
    }

    @Test void capReducedDuringPreparationRejectsWholeBatch() {
        final YamlConfiguration config = new YamlConfiguration();
        when(crate.getFile()).thenReturn(config);
        click(0);
        click(1);
        final List<ItemStack> rewards = prizes.get(0).getItems();
        when(prizes.get(0).getItems()).thenAnswer(call -> {
            config.set("Crate.SelectCrate.MaxSelections", 1);
            return rewards;
        });
        click(49);
        tick();
        verify(users, never()).takeKeys(anyInt(), any(), anyString(), any(), anyBoolean());
        verify(inventory, never()).setStorageContents(any());
        delivery.verifyNoInteractions();
    }

}
