package us.crazycrew.crazycrates.paper.api.support.metrics;

import com.badbones69.crazycrates.paper.api.objects.Crate;
import us.crazycrew.crazycrates.paper.CrazyCrates;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.jetbrains.annotations.NotNull;
import us.crazycrew.crazycrates.api.enums.types.CrateType;
import java.util.ArrayList;
import java.util.List;

public class MetricsWrapper {

    @NotNull
    private final CrazyCrates plugin = CrazyCrates.get();

    private Metrics metrics;

    public void start() {
        if (this.metrics != null) {
            if (this.plugin.isLogging()) this.plugin.getLogger().warning("Metrics is already enabled.");
            return;
        }

        this.metrics = new Metrics(this.plugin, 4514);

        List<Crate> crateList = new ArrayList<>(this.plugin.getCrateManager().getCrates());

        crateList.removeIf(crate -> crate.getCrateType() == CrateType.menu);

        crateList.forEach(crate -> {
            CrateType crateType = crate.getCrateType();

            // If the crate type is null. don't add to the pie chart.
            if (crateType == null) return;

            SimplePie chart = new SimplePie("crate_types", crateType::getName);

            this.metrics.addCustomChart(chart);
        });

        if (this.plugin.isLogging()) this.plugin.getLogger().fine("Metrics has been enabled.");
    }

    public void stop() {
        if (this.metrics == null) {
            if (this.plugin.isLogging()) this.plugin.getLogger().warning("Metrics isn't enabled so we do nothing.");
            return;
        }

        this.metrics.shutdown();
        this.metrics = null;

        if (this.plugin.isLogging()) this.plugin.getLogger().fine("Metrics has been turned off.");
    }
}