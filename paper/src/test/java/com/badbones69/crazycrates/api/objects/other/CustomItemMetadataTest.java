package com.badbones69.crazycrates.api.objects.other;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.utils.MiscUtils;
import com.badbones69.crazycrates.api.utils.MsgUtils;
import de.tr7zw.changeme.nbtapi.NBTItem;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomItemMetadataTest {
    private MockedStatic<CrazyCrates> plugin;
    private MockedStatic<MiscUtils> misc;
    private MockedStatic<MsgUtils> messages;
    private MockedConstruction<ItemStack> stacks;
    private MockedConstruction<NBTItem> nbt;
    private ItemMeta meta;
    private ItemStack providerItem;
    private Component name;
    private List<Component> lore;

    @BeforeEach void setUp() {
        plugin = mockStatic(CrazyCrates.class);
        misc = mockStatic(MiscUtils.class);
        messages = mockStatic(MsgUtils.class);
        messages.when(() -> MsgUtils.color(anyString())).thenAnswer(call -> call.getArgument(0));
        stacks = mockConstruction(ItemStack.class, (item, context) -> {
            when(item.getType()).thenReturn((Material) context.arguments().getFirst());
            when(item.getItemMeta()).thenReturn(mock(ItemMeta.class));
        });
        nbt = mockConstruction(NBTItem.class, (item, context) ->
                when(item.getItem()).thenReturn((ItemStack) context.arguments().getFirst()));
        meta = mock(ItemMeta.class);
        providerItem = mock(ItemStack.class);
        when(providerItem.getType()).thenReturn(Material.BOW);
        when(providerItem.getItemMeta()).thenReturn(meta);
        when(providerItem.getEnchantments()).thenReturn(Map.of());
        name = MiniMessage.miniMessage().deserialize(
                "<gradient:#ffcc33:#ff66cc:#66ccff><bold>ARCO DA PIPA VELOZ</bold></gradient>")
                .decoration(TextDecoration.ITALIC, false).font(Key.key("oraxen", "example"));
        lore = List.of(Component.text("Uma rabiola de luz risca o céu.").decoration(TextDecoration.ITALIC, false));
        when(meta.customName()).thenReturn(name);
        when(meta.itemName()).thenReturn(name);
        when(meta.lore()).thenReturn(lore);
    }

    @AfterEach void tearDown() {
        nbt.close();
        stacks.close();
        messages.close();
        misc.close();
        plugin.close();
    }

    private ItemBuilder builder() throws Exception {
        final ItemBuilder builder = spy(new ItemBuilder().setMaterial(Material.BOW));
        resolveFromProvider(builder);
        return builder;
    }

    private void resolveFromProvider(final ItemBuilder builder) throws Exception {
        final var update = ItemBuilder.class.getDeclaredMethod("updateItemStack", ItemStack.class);
        update.setAccessible(true);
        // Exercise the shared metadata boundary used by the Oraxen and ItemEdit adapters.
        doAnswer(call -> { update.invoke(builder, providerItem); return null; }).when(builder).setCustomItem();
    }

    @Test void nativeDeliveryRetainsRichNamesAndLore() throws Exception {
        final ItemBuilder builder = builder();
        assertSame(providerItem, builder.build());
        verify(meta).customName(name);
        verify(meta).itemName(name);
        verify(meta).lore(lore);
        verify(meta, never()).setDisplayName(any());
        verify(meta, never()).setLore(any());
        assertTrue(builder.getName().contains("§x"), "Text-only announcements must also retain RGB colors");
    }

    @Test void previewReferenceRetainsNameAndAllowsItsOwnLore() throws Exception {
        builder().setName("or:brinquedo_arco").setLore(List.of("Prévia da caixa")).build();
        verify(meta).customName(name);
        verify(meta).itemName(name);
        verify(meta).setLore(List.of("Prévia da caixa"));
    }

    @Test void explicitNameAndEmptyLoreOverrideTheProvider() throws Exception {
        builder().setName("Nome da caixa").setLore(List.of()).build();
        verify(meta).setDisplayName("Nome da caixa");
        verify(meta).setLore(List.of());
    }

    @Test void resolvesPlaceholdersWithoutFlatteningNativeComponents() throws Exception {
        final Component original = Component.text("Dono: %player_name% / {winner}")
                .font(Key.key("oraxen", "example")).decoration(TextDecoration.ITALIC, false);
        when(meta.lore()).thenReturn(List.of(original));
        final Player player = mock(Player.class);
        misc.when(MiscUtils::isPapiActive).thenReturn(true);
        try (var papi = mockStatic(PlaceholderAPI.class)) {
            papi.when(() -> PlaceholderAPI.setPlaceholders(any(Player.class), anyString())).thenAnswer(call -> call.getArgument(1));
            papi.when(() -> PlaceholderAPI.setPlaceholders(player, "%player_name%")).thenReturn("Thiago");
            builder().setTarget(player).addLorePlaceholder("{winner}", "Campeão").build();
            final Component expected = original.replaceText(config -> config.matchLiteral("{winner}").replacement("Campeão"))
                    .replaceText(config -> config.matchLiteral("%player_name%").replacement("Thiago"));
            verify(meta).lore(List.of(expected));
        }
    }

    @Test void copiedBuildersKeepExplicitOverrides() throws Exception {
        final ItemBuilder copy = spy(new ItemBuilder(builder().setName("Customizado").setLore(List.of())));
        resolveFromProvider(copy);
        copy.build();
        verify(meta).setDisplayName("Customizado");
        verify(meta).setLore(List.of());
    }

    @Test void itemNameAloneStillSuppliesTheAnnouncementName() throws Exception {
        when(meta.customName()).thenReturn(null);
        final ItemBuilder builder = builder();
        builder.build();
        verify(meta).customName(null);
        verify(meta).itemName(name);
        assertEquals("ARCO DA PIPA VELOZ", PlainTextComponentSerializer.plainText()
                .serialize(LegacyComponentSerializer.legacySection().deserialize(builder.getName())));
    }

    @Test void absentProviderLoreIsNotReplacedWithAnEmptyOverride() throws Exception {
        when(meta.lore()).thenReturn(null);
        builder().build();
        verify(meta, never()).lore(anyList());
        verify(meta, never()).setLore(any());
    }

    @Test void freshBuildsResolveEachPlayersLore() throws Exception {
        final Component owner = Component.text("Dono: %player_name%").decoration(TextDecoration.ITALIC, false);
        when(meta.lore()).thenReturn(List.of(owner));
        final Player first = mock(Player.class);
        final Player second = mock(Player.class);
        misc.when(MiscUtils::isPapiActive).thenReturn(true);
        try (var papi = mockStatic(PlaceholderAPI.class)) {
            papi.when(() -> PlaceholderAPI.setPlaceholders(any(Player.class), anyString())).thenAnswer(call -> call.getArgument(1));
            papi.when(() -> PlaceholderAPI.setPlaceholders(first, "%player_name%")).thenReturn("Primeiro");
            papi.when(() -> PlaceholderAPI.setPlaceholders(second, "%player_name%")).thenReturn("Segundo");
            final ItemBuilder original = builder();
            for (final Player player : List.of(first, second)) {
                final ItemBuilder copy = spy(new ItemBuilder(original).setTarget(player));
                resolveFromProvider(copy);
                copy.build();
            }
            verify(meta).lore(List.of(owner.replaceText(config -> config.matchLiteral("%player_name%").replacement("Primeiro"))));
            verify(meta).lore(List.of(owner.replaceText(config -> config.matchLiteral("%player_name%").replacement("Segundo"))));
        }
    }
}
