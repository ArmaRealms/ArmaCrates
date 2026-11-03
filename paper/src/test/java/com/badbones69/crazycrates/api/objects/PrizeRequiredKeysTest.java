package com.badbones69.crazycrates.api.objects;

import com.badbones69.crazycrates.api.objects.other.ItemBuilder;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrizeRequiredKeysTest {
    @Test void omittedPriceInheritsGlobalAndExplicitPriceWinsInBothDirections() {
        final YamlConfiguration config = new YamlConfiguration();
        try (var ignored = mockConstruction(ItemBuilder.class)) {
            final Prize prize = new Prize("Example", "1", config);
            assertEquals(1, prize.getRequiredKeys(1));
            assertEquals(2, prize.getRequiredKeys(2));
            config.set("RequiredKeys", 2);
            assertEquals(2, prize.getRequiredKeys(1));
            config.set("RequiredKeys", 1);
            assertEquals(1, prize.getRequiredKeys(2));
        }
    }

    @Test void nonPositivePricesRetainTheExistingMinimumOfOneKey() {
        final YamlConfiguration config = new YamlConfiguration();
        try (var ignored = mockConstruction(ItemBuilder.class)) {
            final Prize prize = new Prize("Example", "1", config);
            assertEquals(1, prize.getRequiredKeys(0));
            config.set("RequiredKeys", 0);
            assertEquals(1, prize.getRequiredKeys(2));
            config.set("RequiredKeys", -1);
            assertEquals(1, prize.getRequiredKeys(2));
        }
    }
}
