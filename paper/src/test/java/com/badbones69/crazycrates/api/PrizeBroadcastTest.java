package com.badbones69.crazycrates.api;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.Prize;
import com.badbones69.crazycrates.api.objects.other.ItemBuilder;
import com.badbones69.crazycrates.api.utils.MiscUtils;
import com.badbones69.crazycrates.api.utils.MsgUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Server;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.mockito.Mockito.*;

class PrizeBroadcastTest {
    private YamlConfiguration broadcast(final Crate crate, final String message) {
        final YamlConfiguration config = new YamlConfiguration();
        config.set("Crate.OpeningBroadCast", true);
        config.set("Crate.BroadCast", message);
        when(crate.getFile()).thenReturn(config);
        return config;
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void broadcastsWithWinnerPlaceholdersAlongsideEitherPrivateMessageSource(final boolean prizeMessage) {
        final CrazyCrates plugin = mock(CrazyCrates.class, RETURNS_DEEP_STUBS);
        final Player winner = mock(Player.class);
        final Player viewer = mock(Player.class);
        final Crate crate = mock(Crate.class);
        final Prize prize = mock(Prize.class);
        final ItemBuilder display = mock(ItemBuilder.class);
        when(winner.getName()).thenReturn("Winner");
        when(crate.getPrizes()).thenReturn(List.of(prize));
        when(crate.getCrateInventoryName()).thenReturn("Halloween");
        when(prize.getDisplayItemBuilder()).thenReturn(display);
        when(display.getName()).thenReturn("&cTAG: %armatags_tag_Dragao%");
        if (prizeMessage) when(prize.getMessages()).thenReturn(List.of("Private reward message"));
        else when(crate.getPrizeMessage()).thenReturn(List.of("Private reward message"));
        final Server server = plugin.getServer();
        doReturn(List.of(winner, viewer)).when(server).getOnlinePlayers();
        broadcast(crate, "%player% won [%reward%] in %crate%");
        try (var singleton = mockStatic(CrazyCrates.class);
             var misc = mockStatic(MiscUtils.class);
             var messages = mockStatic(MsgUtils.class);
             var placeholders = mockStatic(PlaceholderAPI.class)) {
            singleton.when(CrazyCrates::get).thenReturn(plugin);
            messages.when(MsgUtils::getPrefix).thenReturn("");
            misc.when(MiscUtils::isPapiActive).thenReturn(true);
            placeholders.when(() -> PlaceholderAPI.setPlaceholders(winner,
                    "Winner won [&cTAG: %armatags_tag_Dragao%] in Halloween"))
                    .thenReturn("Winner won [&cTAG: Dragão] in Halloween");
            placeholders.when(() -> PlaceholderAPI.setPlaceholders(winner, "Private reward message"))
                    .thenReturn("Private reward message");

            PrizeManager.finishPrize(winner, prize, crate);

            messages.verify(() -> MsgUtils.sendMessage(winner, "Winner won [&cTAG: Dragão] in Halloween", false));
            messages.verify(() -> MsgUtils.sendMessage(viewer, "Winner won [&cTAG: Dragão] in Halloween", false));
            messages.verify(() -> MsgUtils.sendMessage(winner, "Private reward message", false));
            placeholders.verify(() -> PlaceholderAPI.setPlaceholders(eq(viewer), anyString()), never());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Winner opened a crate", "%player% won %reward%"})
    void disabledOrOpeningBroadcastDoesNotAccessTheAudienceOrRewardDisplay(final String message) {
        final CrazyCrates plugin = mock(CrazyCrates.class, RETURNS_DEEP_STUBS);
        final Player winner = mock(Player.class);
        final Prize prize = mock(Prize.class);
        final Crate crate = mock(Crate.class);
        broadcast(crate, message).set("Crate.OpeningBroadCast", !message.contains("%reward%"));
        try (var singleton = mockStatic(CrazyCrates.class)) {
            singleton.when(CrazyCrates::get).thenReturn(plugin);

            PrizeManager.finishPrize(winner, prize, crate);

            verify(prize, never()).getDisplayItemBuilder();
            verify(plugin, never()).getServer();
        }
    }

    @Test void broadcastsMaterialFallbackWithoutPlaceholderApi() {
        final CrazyCrates plugin = mock(CrazyCrates.class, RETURNS_DEEP_STUBS);
        final Player winner = mock(Player.class);
        final Crate crate = mock(Crate.class);
        final Prize prize = mock(Prize.class);
        final ItemBuilder display = mock(ItemBuilder.class, RETURNS_DEEP_STUBS);
        when(winner.getName()).thenReturn("Winner");
        when(crate.getPrizes()).thenReturn(List.of(prize));
        when(crate.getCrateInventoryName()).thenReturn("Halloween");
        when(prize.getDisplayItemBuilder()).thenReturn(display);
        when(display.getName()).thenReturn("");
        when(display.getMaterial().getKey()).thenReturn(NamespacedKey.minecraft("diamond_sword"));
        final Server server = plugin.getServer();
        doReturn(List.of(winner)).when(server).getOnlinePlayers();
        broadcast(crate, "%Player% won %prize%");
        try (var singleton = mockStatic(CrazyCrates.class);
             var misc = mockStatic(MiscUtils.class);
             var messages = mockStatic(MsgUtils.class)) {
            singleton.when(CrazyCrates::get).thenReturn(plugin);
            messages.when(MsgUtils::getPrefix).thenReturn("");
            messages.when(() -> MsgUtils.color("DIAMOND SWORD")).thenReturn("DIAMOND SWORD");

            PrizeManager.finishPrize(winner, prize, crate);

            messages.verify(() -> MsgUtils.sendMessage(winner, "Winner won DIAMOND SWORD", false));
        }
    }

    @Test void alternativeKeyRefundIsNotAnnouncedAsTheOriginalPrize() {
        final CrazyCrates plugin = mock(CrazyCrates.class, RETURNS_DEEP_STUBS);
        final Prize original = mock(Prize.class);
        final Prize refund = mock(Prize.class);
        final Crate crate = mock(Crate.class);
        when(crate.getPrizes()).thenReturn(List.of(original));
        broadcast(crate, "%player% won %reward%");
        try (var singleton = mockStatic(CrazyCrates.class)) {
            singleton.when(CrazyCrates::get).thenReturn(plugin);

            PrizeManager.finishPrize(mock(Player.class), refund, crate);

            verify(refund, never()).getDisplayItemBuilder();
            verify(plugin, never()).getServer();
        }
    }
}
