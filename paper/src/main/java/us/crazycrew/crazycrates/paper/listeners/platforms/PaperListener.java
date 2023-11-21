package us.crazycrew.crazycrates.paper.listeners.platforms;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.jetbrains.annotations.NotNull;
import us.crazycrew.crazycrates.paper.CrazyCrates;
import us.crazycrew.crazycrates.api.enums.types.CrateType;
import us.crazycrew.crazycrates.paper.managers.crates.CrateManager;

public class PaperListener implements Listener {

    @NotNull
    private final CrazyCrates plugin = CrazyCrates.get();

    @NotNull
    private final CrateManager crateManager = this.plugin.getCrateManager();

    @EventHandler(ignoreCancelled = true)
    public void onPlayerAttemptPickUp(PlayerAttemptPickupItemEvent event) {
        if (this.crateManager.isDisplayReward(event.getItem())) {
            event.setCancelled(true);
            return;
        }

        if (this.crateManager.isInOpeningList(event.getPlayer())) {
            // DrBot Start
            if (this.crateManager.getOpeningCrate(event.getPlayer()).getCrateType().equals(CrateType.quick_crate)) return;
            // DrBot End
            event.setCancelled(true);
        }
    }
}