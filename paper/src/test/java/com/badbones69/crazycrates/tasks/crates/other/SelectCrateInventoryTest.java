package com.badbones69.crazycrates.tasks.crates.other;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SelectCrateInventoryTest {
    private final Material material = mock(Material.class);
    // ItemStack needs a running Paper server; these doubles model amount, metadata and stack size.
    private ItemStack item(final String metadata, final int amount, final int max) {
        final ItemStack item = mock(ItemStack.class, metadata);
        final AtomicInteger count = new AtomicInteger(amount);
        when(item.getAmount()).thenAnswer(call -> count.get());
        doAnswer(call -> { count.set(call.getArgument(0)); return null; }).when(item).setAmount(anyInt());
        when(item.getType()).thenReturn(this.material);
        when(item.getMaxStackSize()).thenReturn(max);
        when(item.isSimilar(any())).thenAnswer(call -> metadata.equals(mockingDetails(call.getArgument(0))
                .getMockCreationSettings().getMockName().toString()));
        when(item.clone()).thenAnswer(call -> item(metadata, count.get(), max));
        return item;
    }

    private PlayerInventory inventory(final ItemStack... storage) {
        final PlayerInventory inventory = mock(PlayerInventory.class);
        when(inventory.getStorageContents()).thenReturn(storage);
        final ItemStack empty = item("empty", 0, 64);
        when(inventory.getItemInOffHand()).thenReturn(empty);
        when(inventory.getMaxStackSize()).thenReturn(64);
        return inventory;
    }

    @Test void stacksAllRewardsWithoutMutatingOriginals() {
        final ItemStack existing = item("diamond", 60, 64);
        final PlayerInventory inventory = inventory(existing, null);
        final SelectCrateInventory candidate = new SelectCrateInventory(inventory);
        final ItemStack first = item("diamond", 4, 64);
        assertTrue(candidate.addItems(List.of(first, item("diamond", 64, 64))));
        assertEquals(60, existing.getAmount());
        assertEquals(4, first.getAmount());
        candidate.apply(inventory);
        verify(inventory).setStorageContents(argThat(items -> items[0].getAmount() == 64 && items[1].getAmount() == 64));
    }

    @Test void fullInventoryCanAcceptCompatiblePartialStack() {
        assertTrue(new SelectCrateInventory(inventory(item("same", 63, 64)))
                .addItems(List.of(item("same", 1, 64))));
    }

    @Test void rejectsDifferentMetadataAndDoesNotTouchPlayer() {
        final PlayerInventory inventory = inventory(item("enchanted", 1, 64));
        assertFalse(new SelectCrateInventory(inventory).addItems(List.of(item("plain", 1, 64))));
        verify(inventory, never()).setStorageContents(any());
    }

    @Test void aggregateRewardsMustFitTogether() {
        final PlayerInventory inventory = inventory((ItemStack) null);
        assertFalse(new SelectCrateInventory(inventory).addItems(List.of(item("a", 64, 64), item("b", 1, 64))));
        verify(inventory, never()).setStorageContents(any());
    }

    @Test void honorsUnstackableAndCustomStackLimits() {
        assertFalse(new SelectCrateInventory(inventory(null, null)).addItems(List.of(item("tool", 3, 1))));
        assertTrue(new SelectCrateInventory(inventory(null, null)).addItems(List.of(item("custom", 32, 16))));
        assertFalse(new SelectCrateInventory(inventory(null, null)).addItems(List.of(item("custom", 33, 16))));
    }

    @Test void exactKeyDebitFreesSpaceWithoutRemovingOtherIdenticalStacks() {
        final ItemStack key = item("key", 2, 64);
        final PlayerInventory inventory = inventory(key, key);
        final SelectCrateInventory candidate = new SelectCrateInventory(inventory);
        assertTrue(candidate.takeKeys(2, item -> item.isSimilar(key), false));
        assertTrue(candidate.addItems(List.of(item("reward", 64, 64))));
        candidate.apply(inventory);
        verify(inventory).setStorageContents(argThat(items -> items[0].getAmount() == 64 && items[1].getAmount() == 2));
        assertEquals(2, key.getAmount());
    }

    @Test void insufficientKeysNeverChangesCandidateOrInventory() {
        final ItemStack key = item("key", 2, 64);
        final PlayerInventory inventory = inventory(key);
        final SelectCrateInventory candidate = new SelectCrateInventory(inventory);
        assertFalse(candidate.takeKeys(3, item -> item.isSimilar(key), false));
        assertEquals(2, candidate.countKeys(item -> item.isSimilar(key), false));
        verify(inventory, never()).setStorageContents(any());
    }

    @Test void handOpeningCountsOnlyHeldSlotAndOffhand() {
        final ItemStack key = item("key", 2, 64);
        final PlayerInventory inventory = inventory(key, item("key", 20, 64));
        final ItemStack offhand = item("key", 3, 64);
        when(inventory.getItemInOffHand()).thenReturn(offhand);
        final SelectCrateInventory candidate = new SelectCrateInventory(inventory);
        assertEquals(5, candidate.countKeys(item -> item.isSimilar(key), true));
        assertFalse(candidate.takeKeys(6, item -> item.isSimilar(key), true));
        assertTrue(candidate.takeKeys(5, item -> item.isSimilar(key), true));
        candidate.apply(inventory);
        verify(inventory).setStorageContents(argThat(items -> items[0] == null && items[1].getAmount() == 20));
        verify(inventory).setItemInOffHand(null);
    }
}
