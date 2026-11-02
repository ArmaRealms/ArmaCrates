package com.badbones69.crazycrates.tasks.crates.other;

import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.Prize;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SelectCrateSessionTest {
    @Test void fiveKeysAllowFiveDistinctSelectionsAndDeselectingReleasesOne() {
        final SelectCrateSession session = new SelectCrateSession(mock(Player.class), mock(Crate.class));
        final Prize first = mock(Prize.class);
        assertTrue(session.togglePrize(first, 5));
        for (int i = 0; i < 4; i++) assertTrue(session.togglePrize(mock(Prize.class), 5));
        final Prize sixth = mock(Prize.class);
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
        final Prize prize = mock(Prize.class);
        assertTrue(session.togglePrize(prize, 1));
        assertTrue(session.beginConfirmation());
        assertFalse(session.beginConfirmation());
        assertFalse(session.togglePrize(prize, 1));
        assertEquals(1, session.getSelectedPrizes().size());
    }

    @Test void reducedBalanceStillAllowsDeselectingAndPreventsAdding() {
        final SelectCrateSession session = new SelectCrateSession(mock(Player.class), mock(Crate.class));
        final Prize prize = mock(Prize.class);
        assertTrue(session.togglePrize(prize, 5));
        assertFalse(session.togglePrize(mock(Prize.class), 0));
        assertTrue(session.togglePrize(prize, 0));
        assertFalse(session.hasSelection());
    }
}
