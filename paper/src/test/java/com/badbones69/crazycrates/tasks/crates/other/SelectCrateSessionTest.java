package com.badbones69.crazycrates.tasks.crates.other;

import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.Prize;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyInt;

class SelectCrateSessionTest {
    private Prize prize() {
        final Prize prize = mock(Prize.class);
        when(prize.getRequiredKeys(anyInt())).thenReturn(1);
        return prize;
    }

    @Test void fiveKeysAllowFiveDistinctSelectionsAndDeselectingReleasesOne() {
        final SelectCrateSession session = new SelectCrateSession(mock(Player.class), mock(Crate.class));
        final Prize first = prize();
        assertTrue(session.togglePrize(first, 5));
        for (int i = 0; i < 4; i++) assertTrue(session.togglePrize(prize(), 5));
        final Prize sixth = prize();
        assertFalse(session.togglePrize(sixth, 5));
        assertEquals(5, session.getSelectedPrizes().size());
        assertTrue(session.togglePrize(first, 5));
        assertTrue(session.togglePrize(sixth, 5));
        session.setCurrentPage(1);
        assertEquals(5, session.getSelectedPrizes().size());
    }

    @Test void confirmationIsSingleUseAndLocksSelection() {
        final SelectCrateSession session = new SelectCrateSession(mock(Player.class), mock(Crate.class));
        assertFalse(session.beginConfirmation());
        final Prize prize = prize();
        assertTrue(session.togglePrize(prize, 1));
        assertTrue(session.beginConfirmation());
        assertFalse(session.beginConfirmation());
        assertFalse(session.togglePrize(prize, 1));
        assertEquals(1, session.getSelectedPrizes().size());
    }

    @Test void reducedBalanceStillAllowsDeselectingAndPreventsAdding() {
        final SelectCrateSession session = new SelectCrateSession(mock(Player.class), mock(Crate.class));
        final Prize prize = prize();
        assertTrue(session.togglePrize(prize, 5));
        assertFalse(session.togglePrize(prize(), 0));
        assertTrue(session.togglePrize(prize, 0));
        assertFalse(session.hasSelection());
    }
    @Test void mixedPricesRespectBudgetAndDeselectingRestoresIt() {
        final Crate crate = mock(Crate.class);
        final SelectCrateSession session = new SelectCrateSession(mock(Player.class), crate);
        final Prize expensive = prize();
        final Prize cheap = prize();
        when(expensive.getRequiredKeys(anyInt())).thenReturn(2);
        assertTrue(session.togglePrize(expensive, 2));
        assertFalse(session.togglePrize(cheap, 2));
        assertTrue(session.togglePrize(expensive, 2));
        assertTrue(session.togglePrize(cheap, 2));
        assertEquals(1, SelectCrateSession.getSelectionCost(crate, session.getSelectedPrizes(), false));
    }

    @Test void aggregateCostDoesNotOverflowAndFreeOpeningsStillAllowOnlyOneChoice() {
        final Crate crate = mock(Crate.class);
        final SelectCrateSession session = new SelectCrateSession(mock(Player.class), crate);
        final Prize first = prize();
        final Prize second = prize();
        when(first.getRequiredKeys(anyInt())).thenReturn(Integer.MAX_VALUE);
        when(second.getRequiredKeys(anyInt())).thenReturn(Integer.MAX_VALUE);
        assertEquals(4294967294L, SelectCrateSession.getSelectionCost(crate, java.util.List.of(first, second), false));
        assertTrue(session.togglePrize(first, Integer.MAX_VALUE));
        assertFalse(session.togglePrize(second, Integer.MAX_VALUE));
        assertTrue(session.togglePrize(first, 0));
        assertTrue(session.togglePrize(first, 1, true));
        assertFalse(session.togglePrize(second, 1, true));
    }

    @Test void defaultSelectionCapIsFiveEvenWithPlentyOfKeys() {
        final SelectCrateSession session = new SelectCrateSession(mock(Player.class), mock(Crate.class));
        for (int i = 0; i < 5; i++) assertTrue(session.togglePrize(prize(), 100));
        assertFalse(session.togglePrize(prize(), 100));
    }

    @Test void configuredCapCanBeLowerOrHigherAndDeselectingStillWorks() {
        for (final int limit : java.util.List.of(1, 3, 8)) {
            final Crate crate = mock(Crate.class);
            final YamlConfiguration config = new YamlConfiguration();
            config.set("Crate.SelectCrate.MaxSelections", limit);
            when(crate.getFile()).thenReturn(config);
            final SelectCrateSession session = new SelectCrateSession(mock(Player.class), crate);
            final Prize first = prize();
            assertTrue(session.togglePrize(first, 100));
            for (int i = 1; i < limit; i++) assertTrue(session.togglePrize(prize(), 100));
            assertFalse(session.togglePrize(prize(), 100));
            assertTrue(session.togglePrize(first, 0));
            assertTrue(session.togglePrize(prize(), 100));
        }
    }

    @Test void nonPositiveCapsUseOneAndFreeOpeningsKeepTheirSingleChoice() {
        final Crate crate = mock(Crate.class);
        final YamlConfiguration config = new YamlConfiguration();
        when(crate.getFile()).thenReturn(config);
        for (final int configured : java.util.List.of(0, -1)) {
            config.set("Crate.SelectCrate.MaxSelections", configured);
            assertEquals(1, SelectCrateSession.getMaxSelections(crate, false));
        }
        config.set("Crate.SelectCrate.MaxSelections", 8);
        assertEquals(1, SelectCrateSession.getMaxSelections(crate, true));
    }

}
