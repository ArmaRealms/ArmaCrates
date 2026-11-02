package com.badbones69.crazycrates.tasks.crates.types;

import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.Prize;
import com.badbones69.crazycrates.api.objects.other.ItemBuilder;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SelectCrateBlockedPrizeTest {
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
            SelectCrate.renderPage(inventory, player, crate, 0, Set.of(prize), 1);
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
}
