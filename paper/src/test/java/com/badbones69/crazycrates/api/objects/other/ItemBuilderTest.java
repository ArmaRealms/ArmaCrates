package com.badbones69.crazycrates.api.objects.other;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class ItemBuilderTest {
    @Test void buildsTheResolvedCustomItemOnTheFirstCall() {
        try (final var stacks = mockConstruction(ItemStack.class, (item, context) -> {
            when(item.getType()).thenReturn((Material) context.arguments().getFirst());
            final ItemMeta meta = mock(ItemMeta.class);
            when(item.getItemMeta()).thenReturn(meta);
        });
             final var nbt = mockConstruction(NBTItem.class, (item, context) ->
                     when(item.getItem()).thenReturn((ItemStack) context.arguments().getFirst()))) {
            final ItemBuilder builder = spy(new ItemBuilder().setMaterial(Material.STONE).setAmount(2));
            doReturn("").when(builder).getUpdatedName();
            doReturn(List.of()).when(builder).getUpdatedLore();
            // Simulate the provider replacing the fallback stack during custom item resolution.
            doAnswer(invocation -> {
                builder.setMaterial(Material.DIAMOND_SWORD);
                return null;
            }).when(builder).setCustomItem();

            final ItemStack result = builder.build();
            final ItemStack resolved = stacks.constructed().get(1);
            assertSame(resolved, result);
            verify(resolved).setAmount(2);
            verify(stacks.constructed().getFirst(), never()).setAmount(anyInt());
        }
    }
}
