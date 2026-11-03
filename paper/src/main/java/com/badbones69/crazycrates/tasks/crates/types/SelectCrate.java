package com.badbones69.crazycrates.tasks.crates.types;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.builders.CrateBuilder;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.Prize;
import com.badbones69.crazycrates.api.objects.other.ItemBuilder;
import com.badbones69.crazycrates.api.utils.ItemUtils;
import com.badbones69.crazycrates.api.utils.MsgUtils;
import com.badbones69.crazycrates.tasks.crates.other.SelectCrateInventory;
import com.badbones69.crazycrates.tasks.crates.other.SelectCrateSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import us.crazycrew.crazycrates.api.enums.types.KeyType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SelectCrate allows players to choose which prize they want to receive.
 * Players select distinct prizes up to their key balance before confirming.
 */
public class SelectCrate extends CrateBuilder {

    /** Default slot immediately to the left of the confirmation button. */
    public static final int DEFAULT_PREVIOUS_PAGE_SLOT = 48;
    /** Default central slot for confirming the selected prize. */
    public static final int DEFAULT_CONFIRM_SLOT = 49;
    /** Default slot immediately to the right of the confirmation button. */
    public static final int DEFAULT_NEXT_PAGE_SLOT = 50;

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
    private static @NonNull String getGuiTitle(final @NonNull Crate crate) {
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

        renderPage(getInventory(), getPlayer(), getCrate(), 0, Set.of(),
                getAvailableKeys(getPlayer(), getCrate(), type, checkHand), type == KeyType.free_key);

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

    /** Returns the available key budget from the opening key source. */
    public static int getAvailableKeys(final Player player, final Crate crate, final KeyType type,
                                        final boolean checkHand) {
        if (type == null) return 0;
        if (type == KeyType.free_key) return 1;
        final int keys = type == KeyType.virtual_key
                ? CrazyCrates.get().getUserManager().getVirtualKeys(player.getUniqueId(), crate.getName())
                : new SelectCrateInventory(player.getInventory()).countKeys(
                        item -> ItemUtils.isSimilar(item, crate), checkHand);
        return Math.max(0, keys);
    }

    /**
     * Gets the confirm button from config or creates a default one.
     *
     * @param file the crate configuration file
     * @return the confirm button ItemBuilder
     */
    private static @NonNull ItemBuilder getConfirmButton(final FileConfiguration file) {
        final ItemBuilder builder = new ItemBuilder();

        if (file != null && file.contains("Crate.SelectCrate.Confirm.Item")) {
            final String material = file.getString("Crate.SelectCrate.Confirm.Item.Material", "LIME_CONCRETE");
            final String name = file.getString("Crate.SelectCrate.Confirm.Item.Name", "&aConfirmar escolha");
            List<String> lore = file.getStringList("Crate.SelectCrate.Confirm.Item.Lore");

            if (lore.isEmpty()) {
                lore = new ArrayList<>();
                lore.add("&7Click to receive all selected prizes.");
            }

            builder.setMaterial(material)
                    .setName(MsgUtils.color(name))
                    .setLore(lore);
        } else {
            // Default confirm button
            builder.setMaterial(Material.LIME_CONCRETE)
                    .setName(MsgUtils.color("&aConfirm Choice"))
                    .setLore(List.of(MsgUtils.color("&7Click to receive all selected prizes.")));
        }

        return builder;
    }

    /**
     * Renders one page of prizes and the selection controls in the supplied inventory.
     *
     * @param inventory    inventory that is currently displayed
     * @param player       player viewing the prizes
     * @param crate        crate whose prizes should be rendered
     * @param page         zero-based page index
     * @param selectedPrizes prizes selected by the player
     * @param availableKeys current key balance
     * @param free whether this opening allows one choice without charging keys
     */
    public static void renderPage(final @NonNull Inventory inventory, final @NonNull Player player,
                                  final @NonNull Crate crate, final int page, final Set<Prize> selectedPrizes, final int availableKeys, final boolean free) {
        final FileConfiguration file = crate.getFile();
        final int prizesPerPage = getPrizesPerPage(inventory.getSize());
        final int pageCount = getPageCount(crate, inventory.getSize());
        final int safePage = Math.clamp(page, 0, pageCount - 1);

        inventory.clear();

        final int maxSelections = SelectCrateSession.getMaxSelections(crate, free);
        final long selectedCost = SelectCrateSession.getSelectionCost(crate, selectedPrizes, free);
        final List<Prize> prizes = crate.getPrizes();
        final int firstPrize = safePage * prizesPerPage;
        final int lastPrize = Math.min(firstPrize + prizesPerPage, prizes.size());

        for (int prizeIndex = firstPrize; prizeIndex < lastPrize; prizeIndex++) {
            final Prize prize = prizes.get(prizeIndex);
            final int slot = prizeIndex - firstPrize;
            if (prize.hasPermission(player)) {
                inventory.setItem(slot, getBlockedPrize(file).build());
                continue;
            }
            final ItemStack item = selectedPrizes.contains(prize)
                    ? getSelectionMarker(file).build()
                    : prize.getDisplayItem(player);
            final int prizeCost = free ? 1 : prize.getRequiredKeys(crate.getRequiredKeys());
            item.editMeta(meta -> {
                final List<Component> originalLore = meta.lore();
                final List<Component> lore = originalLore == null ? new ArrayList<>() : new ArrayList<>(originalLore);
                final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
                lore.add(serializer.deserialize(message(crate, "PrizeCost", "&7Custo: %cost% chave(s).")
                        .replace("%cost%", Integer.toString(free ? 0 : prizeCost)))
                        .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
                if (!selectedPrizes.contains(prize) && selectedPrizes.size() >= maxSelections) {
                    lore.add(serializer.deserialize(message(crate, "MaxSelections", "&cVocê pode selecionar no máximo %max% prêmios.")
                            .replace("%max%", Integer.toString(maxSelections)))
                            .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
                } else if (!selectedPrizes.contains(prize) && selectedCost + prizeCost > availableKeys) {
                    lore.add(serializer.deserialize(message(crate, "Unavailable", "&cSaldo insuficiente. Desmarque outro produto."))
                            .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
                }
                meta.lore(lore);
            });
            inventory.setItem(slot, item);
        }

        addGlass(inventory, file);

        final int confirmSlot = getControlSlot(crate, inventory.getSize(), "Confirm");
        final String counter = message(crate, "Counter", "&eSelecionados: %selected%/%max% | Chaves: %cost%/%keys%")
                .replace("%selected%", Integer.toString(selectedPrizes.size()))
                .replace("%max%", Integer.toString(maxSelections))
                .replace("%cost%", Long.toString(free ? 0 : selectedCost))
                .replace("%keys%", Integer.toString(free ? 0 : availableKeys))
                .replace("%limit%", Integer.toString(availableKeys));
        setItemIfValid(inventory, confirmSlot, getConfirmButton(file).addLore(counter).build());
        final int cancelSlot = getControlSlot(crate, inventory.getSize(), "Cancel");
        setItemIfValid(inventory, cancelSlot, new ItemBuilder().setMaterial(Material.BARRIER)
                .setName(message(crate, "Cancel", "&cCancelar")).build());

        if (pageCount > 1) {
            final int previousSlot = getControlSlot(crate, inventory.getSize(), "Navigation.Previous");
            final int nextSlot = getControlSlot(crate, inventory.getSize(), "Navigation.Next");
            setItemIfValid(inventory, previousSlot, getNavigationButton(file, false, safePage, pageCount).build());
            setItemIfValid(inventory, nextSlot, getNavigationButton(file, true, safePage, pageCount).build());
        }
    }

    /** Keeps controls distinct and outside prize slots, also for smaller inventories. */
    public static int getControlSlot(final Crate crate, final int size, final String control) {
        final String[] controls = {"Confirm", "Navigation.Previous", "Navigation.Next", "Cancel"};
        final int[] defaults = {size - 5, size - 6, size - 4, size - 1};
        final Set<Integer> used = new HashSet<>();
        for (int index = 0; index < controls.length; index++) {
            int slot = crate.getFile() == null ? defaults[index]
                    : crate.getFile().getInt("Crate.SelectCrate." + controls[index] + ".Slot", defaults[index]);
            if (slot < getPrizesPerPage(size) || slot >= size || used.contains(slot)) {
                slot = defaults[index];
                while (slot < getPrizesPerPage(size) || used.contains(slot)) {
                    slot = (slot + 1) % size;
                }
            }
            if (controls[index].equals(control)) return slot;
            used.add(slot);
        }
        throw new IllegalArgumentException("Unknown SelectCrate control: " + control);
    }

    /** Resolves a configurable SelectCrate message using the existing color pipeline. */
    public static String message(final Crate crate, final String key, final String fallback) {
        return MsgUtils.color(crate.getFile() == null ? fallback
                : crate.getFile().getString("Crate.SelectCrate.Messages." + key, fallback));
    }

    /**
     * Calculates how many pages are required to display every prize.
     *
     * @param crate         crate whose prizes will be displayed
     * @param inventorySize size of the selection inventory
     * @return number of pages, always at least one
     */
    public static int getPageCount(final @NonNull Crate crate, final int inventorySize) {
        final int prizesPerPage = getPrizesPerPage(inventorySize);
        return Math.max(1, (crate.getPrizes().size() + prizesPerPage - 1) / prizesPerPage);
    }

    /**
     * Calculates the number of prize slots while reserving the bottom row for controls.
     *
     * @param inventorySize size of the selection inventory
     * @return number of slots available for prizes
     */
    public static int getPrizesPerPage(final int inventorySize) {
        return Math.max(1, inventorySize - 9);
    }

    /**
     * Adds configured decorative panes only to currently empty slots.
     *
     * @param inventory inventory receiving the panes
     * @param file      crate configuration, or {@code null}
     */
    private static void addGlass(final Inventory inventory, final FileConfiguration file) {
        if (file == null || !file.getBoolean("Crate.SelectCrate.Glass.Toggle", false)) {
            return;
        }

        final String material = file.getString("Crate.SelectCrate.Glass.Item", "GRAY_STAINED_GLASS_PANE");
        final String name = MsgUtils.color(file.getString("Crate.SelectCrate.Glass.Name", ""));
        final ItemStack glass = new ItemBuilder().setMaterial(material).setName(name).build();

        for (final int slot : file.getIntegerList("Crate.SelectCrate.Glass.Slots")) {
            if (slot >= 0 && slot < inventory.getSize()) {
                final ItemStack existing = inventory.getItem(slot);
                if (existing == null || existing.getType() == Material.AIR) {
                    inventory.setItem(slot, glass);
                }
            }
        }
    }

    /**
     * Creates a configurable previous- or next-page button.
     *
     * @param file      crate configuration, or {@code null}
     * @param next      whether the button advances instead of returning
     * @param page      current zero-based page index
     * @param pageCount total number of pages
     * @return configured navigation button
     */
    private static @NonNull ItemBuilder getNavigationButton(final FileConfiguration file, final boolean next,
                                                            final int page, final int pageCount) {
        final String direction = next ? "Next" : "Previous";
        final String defaultMaterial = "ARROW";
        final String defaultName = next ? "&aNext Page" : "&ePrevious Page";
        final String path = "Crate.SelectCrate.Navigation." + direction + ".Item";
        final String material = file != null ? file.getString(path + ".Material", defaultMaterial) : defaultMaterial;
        final String name = file != null ? file.getString(path + ".Name", defaultName) : defaultName;
        List<String> lore = file != null ? file.getStringList(path + ".Lore") : List.of();

        if (lore.isEmpty()) {
            lore = List.of("&7Page " + (page + 1) + " of " + pageCount);
        } else {
            lore = lore.stream()
                    .map(line -> line.replace("%page%", Integer.toString(page + 1))
                            .replace("%pages%", Integer.toString(pageCount)))
                    .toList();
        }

        return new ItemBuilder().setMaterial(material).setName(MsgUtils.color(name)).setLore(lore);
    }

    /**
     * Places an item only when its configured slot belongs to the inventory.
     *
     * @param inventory destination inventory
     * @param slot      desired slot
     * @param item      item to place
     */
    private static void setItemIfValid(final Inventory inventory, final int slot, final ItemStack item) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
        }
    }

    /** Creates the configurable icon used instead of a blacklisted prize. */
    public static ItemBuilder getBlockedPrize(final FileConfiguration file) {
        final String path = "Crate.SelectCrate.BlockedPrize.";
        final String material = file == null ? "STRUCTURE_VOID" : file.getString(path + "Material", "STRUCTURE_VOID");
        final String name = file == null ? "&cPrêmio indisponível" : file.getString(path + "Name", "&cPrêmio indisponível");
        final List<String> defaultLore = List.of("&7Você não pode selecionar este prêmio.");
        final List<String> lore = file != null && file.contains(path + "Lore") ? file.getStringList(path + "Lore") : defaultLore;
        return new ItemBuilder().setMaterial(material).setName(MsgUtils.color(name)).setLore(lore);
    }

    /**
     * Gets the selection marker item from config or creates a default one.
     *
     * @param file the crate configuration file
     * @return the selection marker ItemBuilder
     */
    public static @NonNull ItemBuilder getSelectionMarker(final FileConfiguration file) {
        final ItemBuilder builder = new ItemBuilder();

        if (file != null && file.contains("Crate.SelectCrate.SelectionMarker")) {
            final String material = file.getString("Crate.SelectCrate.SelectionMarker.Material", "NETHER_STAR");
            final String name = file.getString("Crate.SelectCrate.SelectionMarker.Name", "&eSelected");
            final List<String> lore = file.getStringList("Crate.SelectCrate.SelectionMarker.Lore");

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
