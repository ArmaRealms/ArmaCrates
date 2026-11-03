package com.badbones69.crazycrates.tasks.crates.types;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.Prize;
import com.badbones69.crazycrates.api.objects.other.ItemBuilder;
import com.badbones69.crazycrates.api.utils.MsgUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SelectCrateBlockedPrizeTest {
    private MockedStatic<CrazyCrates> plugin;
    private MockedStatic<MsgUtils> messages;

    @BeforeEach void setUp() {
        plugin = mockStatic(CrazyCrates.class);
        messages = mockStatic(MsgUtils.class);
        messages.when(() -> MsgUtils.color(anyString())).thenAnswer(call -> call.getArgument(0));
    }

    @AfterEach void tearDown() {
        messages.close();
        plugin.close();
    }

    @Test void blockedPrizeUsesStructureVoidInsteadOfDisplayOrSelectionMarker() {
        final Crate crate = mock(Crate.class);
        final Prize prize = mock(Prize.class);
        final Player player = mock(Player.class);
        final Inventory inventory = mock(Inventory.class);
        final ItemStack icon = mock(ItemStack.class);
        when(crate.getPrizes()).thenReturn(List.of(prize));
        when(prize.hasPermission(player)).thenReturn(true);
        when(inventory.getSize()).thenReturn(54);
        try (var builders = mockConstruction(ItemBuilder.class, withSettings().defaultAnswer(RETURNS_SELF),
                (builder, context) -> when(builder.build()).thenReturn(icon))) {
            SelectCrate.renderPage(inventory, player, crate, 0, Set.of(prize), 1, false);
            final ItemBuilder blocked = builders.constructed().getFirst();
            verify(blocked).setMaterial("STRUCTURE_VOID");
            verify(inventory).setItem(0, icon);
            verify(prize, never()).getDisplayItem(any(Player.class));
        }
    }

    @Test void blockedIconUsesConfiguredMaterialNameAndLore() {
        final YamlConfiguration config = new YamlConfiguration();
        config.set("Crate.SelectCrate.BlockedPrize.Material", "BARRIER");
        config.set("Crate.SelectCrate.BlockedPrize.Name", "Custom blocked reward");
        config.set("Crate.SelectCrate.BlockedPrize.Lore", List.of("Already claimed"));
        try (var builders = mockConstruction(ItemBuilder.class, withSettings().defaultAnswer(RETURNS_SELF))) {
            SelectCrate.getBlockedPrize(config);
            final ItemBuilder blocked = builders.constructed().getFirst();
            verify(blocked).setMaterial("BARRIER");
            verify(blocked).setName("Custom blocked reward");
            verify(blocked).setLore(List.of("Already claimed"));
        }
    }
    @Test void displayUsesRemainingKeyBudgetAndShowsIndividualCostAndTotal() {
        final Crate crate = mock(Crate.class);
        final Prize selected = mock(Prize.class);
        final Prize expensive = mock(Prize.class);
        final Player player = mock(Player.class);
        final Inventory inventory = mock(Inventory.class);
        final ItemStack display = mock(ItemStack.class);
        final ItemMeta meta = mock(ItemMeta.class);
        final Component nativeLore = Component.text("Lore do Oraxen").font(Key.key("oraxen", "example"))
                .decoration(TextDecoration.ITALIC, false);
        when(meta.lore()).thenReturn(List.of(nativeLore));
        final ItemStack icon = mock(ItemStack.class);
        when(crate.getPrizes()).thenReturn(List.of(selected, expensive));
        when(selected.getRequiredKeys(anyInt())).thenReturn(1);
        when(expensive.getRequiredKeys(anyInt())).thenReturn(2);
        when(expensive.getDisplayItem(player)).thenReturn(display);
        when(inventory.getSize()).thenReturn(54);
        doAnswer(call -> { ((Consumer<ItemMeta>) call.getArgument(0)).accept(meta); return true; })
                .when(display).editMeta(any(Consumer.class));
        try (var builders = mockConstruction(ItemBuilder.class, withSettings().defaultAnswer(RETURNS_SELF),
                (builder, context) -> when(builder.build()).thenReturn(icon))) {
            SelectCrate.renderPage(inventory, player, crate, 0, Set.of(selected), 2, false);
            verify(meta).lore(argThat(lore -> lore.getFirst().equals(nativeLore)
                    && lore.get(1).decoration(TextDecoration.ITALIC) == TextDecoration.State.FALSE
                    && lore.stream().map(PlainTextComponentSerializer.plainText()::serialize).anyMatch(line -> line.contains("Custo: 2"))
                    && lore.stream().map(PlainTextComponentSerializer.plainText()::serialize).anyMatch(line -> line.contains("Saldo insuficiente"))));
            verify(builders.constructed().get(1)).addLore(contains("Chaves: 1/2"));
        }
    }

    @Test void configuredCapShowsUnavailableNoticeEvenWithEnoughKeys() {
        final Crate crate = mock(Crate.class);
        final Prize selected = mock(Prize.class);
        final Prize other = mock(Prize.class);
        final Player player = mock(Player.class);
        final Inventory inventory = mock(Inventory.class);
        final ItemStack display = mock(ItemStack.class);
        final ItemMeta meta = mock(ItemMeta.class);
        final ItemStack icon = mock(ItemStack.class);
        final YamlConfiguration config = new YamlConfiguration();
        config.set("Crate.SelectCrate.MaxSelections", 1);
        when(crate.getFile()).thenReturn(config);
        when(crate.getPrizes()).thenReturn(List.of(selected, other));
        when(selected.getRequiredKeys(anyInt())).thenReturn(1);
        when(other.getRequiredKeys(anyInt())).thenReturn(1);
        when(other.getDisplayItem(player)).thenReturn(display);
        when(inventory.getSize()).thenReturn(54);
        doAnswer(call -> { ((Consumer<ItemMeta>) call.getArgument(0)).accept(meta); return true; })
                .when(display).editMeta(any(Consumer.class));
        try (var builders = mockConstruction(ItemBuilder.class, withSettings().defaultAnswer(RETURNS_SELF),
                (builder, context) -> when(builder.build()).thenReturn(icon))) {
            SelectCrate.renderPage(inventory, player, crate, 0, Set.of(selected), 100, false);
            verify(meta).lore(argThat(lore -> lore.stream().map(PlainTextComponentSerializer.plainText()::serialize)
                    .anyMatch(line -> line.contains("no máximo 1"))));
            verify(builders.constructed().get(1)).addLore(contains("Selecionados: 1/1"));
        }
    }

}
