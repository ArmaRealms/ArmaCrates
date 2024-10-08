package com.badbones69.crazycrates.listeners;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.objects.other.BrokeLocation;
import com.badbones69.crazycrates.api.objects.other.CrateLocation;
import com.badbones69.crazycrates.tasks.crates.CrateManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

// Only use for this class is to check if for broken locations and to try and fix them when the server loads the world.
public class BrokeLocationsListener implements Listener {

    @NotNull
    private final CrazyCrates plugin = CrazyCrates.get();

    @NotNull
    private final CrateManager crateManager = this.plugin.getCrateManager();

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (this.crateManager.getBrokeLocations().isEmpty()) {
            return;
        }

        int fixedAmount = 0;
        List<BrokeLocation> fixedWorlds = new ArrayList<>();

        for (BrokeLocation brokeLocation : this.crateManager.getBrokeLocations()) {
            Location location = brokeLocation.getLocation();

            if (location.getWorld() != null && (brokeLocation.getCrate() != null)) {
                this.crateManager.addLocation(new CrateLocation(brokeLocation.getLocationName(), brokeLocation.getCrate(), location));

                if (brokeLocation.getCrate().getHologram().isEnabled() && this.crateManager.getHolograms() != null) {
                    this.crateManager.getHolograms().createHologram(location.getBlock(), brokeLocation.getCrate());
                }

                fixedWorlds.add(brokeLocation);
                fixedAmount++;
            }
        }

        this.crateManager.removeBrokeLocation(fixedWorlds);

        int finalFixedAmount = fixedAmount;
        this.plugin.debug(() -> "Fixed " + finalFixedAmount + " broken crate locations.", Level.WARNING);

        if (this.crateManager.getBrokeLocations().isEmpty()) {
            this.plugin.debug(() -> "All broken crate locations have been fixed.", Level.WARNING);
        }
    }
}