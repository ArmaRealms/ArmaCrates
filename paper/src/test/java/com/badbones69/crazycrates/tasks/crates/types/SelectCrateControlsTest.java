package com.badbones69.crazycrates.tasks.crates.types;

import com.badbones69.crazycrates.api.objects.Crate;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SelectCrateControlsTest {
    @Test void controlsStayDistinctAndOutsidePrizesForEverySupportedSize() {
        final Crate crate = mock(Crate.class);
        final YamlConfiguration config = new YamlConfiguration();
        when(crate.getFile()).thenReturn(config);
        for (final int size : List.of(9, 18, 27, 36, 45, 54)) {
            for (final boolean conflicting : List.of(false, true)) {
                config.set("Crate.SelectCrate.Confirm.Slot", conflicting ? 53 : 0);
                config.set("Crate.SelectCrate.Cancel.Slot", 53);
                config.set("Crate.SelectCrate.Navigation.Previous.Slot", -1);
                config.set("Crate.SelectCrate.Navigation.Next.Slot", 100);
                final HashSet<Integer> used = new HashSet<>();
                for (final String control : List.of("Confirm", "Navigation.Previous", "Navigation.Next", "Cancel")) {
                    final int slot = SelectCrate.getControlSlot(crate, size, control);
                    assertTrue(slot >= SelectCrate.getPrizesPerPage(size) && slot < size);
                    assertTrue(used.add(slot));
                }
            }
        }
    }
}
