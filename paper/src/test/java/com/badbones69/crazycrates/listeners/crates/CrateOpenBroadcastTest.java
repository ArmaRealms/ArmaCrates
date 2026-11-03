package com.badbones69.crazycrates.listeners.crates;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.events.CrateOpenEvent;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.utils.MsgUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import us.crazycrew.crazycrates.api.enums.types.CrateType;
import us.crazycrew.crazycrates.api.enums.types.KeyType;

import static org.mockito.Mockito.*;

class CrateOpenBroadcastTest {
    @ParameterizedTest
    @ValueSource(strings = {"%player% won %reward%", "%player% won %prize%", "%player% opened a crate"})
    void defersOnlyMessagesThatNeedTheDeliveredPrize(final String message) {
        final CrazyCrates plugin = mock(CrazyCrates.class, RETURNS_DEEP_STUBS);
        final Player player = mock(Player.class);
        final Crate crate = mock(Crate.class);
        final YamlConfiguration config = new YamlConfiguration();
        config.set("Crate.OpeningBroadCast", true);
        config.set("Crate.BroadCast", message);
        when(crate.getName()).thenReturn("test");
        when(crate.getCrateType()).thenReturn(CrateType.select_crate);
        when(crate.canWinPrizes(player)).thenReturn(true);
        when(player.hasPermission(anyString())).thenReturn(true);
        when(player.getName()).thenReturn("Winner");
        try (var singleton = mockStatic(CrazyCrates.class);
             var messages = mockStatic(MsgUtils.class)) {
            singleton.when(CrazyCrates::get).thenReturn(plugin);
            messages.when(MsgUtils::getPrefix).thenReturn("");
            messages.when(() -> MsgUtils.color(anyString())).thenAnswer(call -> call.getArgument(0));
            final CrateOpenListener listener = new CrateOpenListener();

            listener.onCrateOpen(new CrateOpenEvent(player, crate, KeyType.virtual_key, false, config));

            if (message.contains("opened")) verify(plugin.getServer()).broadcastMessage("Winner opened a crate");
            else verify(plugin, never()).getServer();
        }
    }
}
