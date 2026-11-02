package com.badbones69.crazycrates.tasks.crates.other;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/** A detached inventory candidate: failed validation never changes the player's items. */
public final class SelectCrateInventory {
    private final ItemStack[] storage;
    private ItemStack offhand;
    private final int heldSlot;
    private final int maxStackSize;

    public SelectCrateInventory(final PlayerInventory inventory) {
        this.storage = Arrays.stream(inventory.getStorageContents())
                .map(item -> item == null ? null : item.clone()).toArray(ItemStack[]::new);
        this.offhand = inventory.getItemInOffHand().clone();
        this.heldSlot = inventory.getHeldItemSlot();
        this.maxStackSize = inventory.getMaxStackSize();
    }

    private static boolean isUsable(final ItemStack item) {
        return item != null && item.getAmount() > 0 && !item.getType().isAir();
    }

    public int countKeys(final Predicate<ItemStack> isKey, final boolean checkHand) {
        long count = isUsable(this.offhand) && isKey.test(this.offhand) ? this.offhand.getAmount() : 0;
        for (int slot = 0; slot < this.storage.length; slot++) {
            final ItemStack item = this.storage[slot];
            if ((!checkHand || slot == this.heldSlot) && isUsable(item) && isKey.test(item)) {
                count += item.getAmount();
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, count);
    }

    /** Removes exactly the requested keys, including offhand, after checking the whole balance. */
    public boolean takeKeys(final int amount, final Predicate<ItemStack> isKey, final boolean checkHand) {
        if (amount < 1 || countKeys(isKey, checkHand) < amount) return false;
        int remaining = amount;
        for (int slot = 0; slot < this.storage.length && remaining > 0; slot++) {
            final ItemStack item = this.storage[slot];
            if ((!checkHand || slot == this.heldSlot) && isUsable(item) && isKey.test(item)) {
                final int taken = Math.min(remaining, item.getAmount());
                remaining -= taken;
                if (taken == item.getAmount()) this.storage[slot] = null;
                else item.setAmount(item.getAmount() - taken);
            }
        }
        if (remaining > 0) {
            if (remaining == this.offhand.getAmount()) this.offhand = null;
            else this.offhand.setAmount(this.offhand.getAmount() - remaining);
        }
        return true;
    }

    /** Packs actual reward stacks, respecting metadata and per-item stack limits. */
    public boolean addItems(final List<ItemStack> rewards) {
        for (final ItemStack reward : rewards) {
            if (reward == null || reward.getType().isAir() || reward.getAmount() < 1) {
                throw new IllegalArgumentException("Invalid SelectCrate reward item");
            }
            int remaining = reward.getAmount();
            final int limit = Math.min(this.maxStackSize, reward.getMaxStackSize());
            if (limit < 1) throw new IllegalArgumentException("Invalid SelectCrate stack limit");
            for (final ItemStack item : this.storage) {
                if (item != null && item.isSimilar(reward)) {
                    final int added = Math.min(remaining, Math.max(0, limit - item.getAmount()));
                    item.setAmount(item.getAmount() + added);
                    remaining -= added;
                    if (remaining == 0) break;
                }
            }
            for (int slot = 0; slot < this.storage.length && remaining > 0; slot++) {
                if (this.storage[slot] == null || this.storage[slot].getType().isAir()) {
                    final ItemStack item = reward.clone();
                    item.setAmount(Math.min(remaining, limit));
                    this.storage[slot] = item;
                    remaining -= item.getAmount();
                }
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    /** Publishes the validated candidate without calling commands or plugin events between slots. */
    public void apply(final PlayerInventory inventory) {
        inventory.setStorageContents(this.storage);
        inventory.setItemInOffHand(this.offhand);
    }
}
