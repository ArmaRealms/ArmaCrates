package com.badbones69.crazycrates;

import com.badbones69.crazycrates.api.FileManager;
import com.badbones69.crazycrates.api.FileManager.Files;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.tasks.BukkitUserManager;
import com.badbones69.crazycrates.tasks.crates.CrateManager;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import us.crazycrew.crazycrates.api.enums.types.KeyType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ConcurrentKeyDeliveryTest {
    @TempDir
    Path directory;

    @Test
    void savesOverlappingDeliveriesWithoutLosingOrDuplicatingBalances() throws Exception {
        final CrazyCrates plugin = mock(CrazyCrates.class);
        final Server server = mock(Server.class);
        final CrateManager crateManager = mock(CrateManager.class);
        final AsyncScheduler scheduler = mock(AsyncScheduler.class);
        final Player player = mock(Player.class);
        final Crate crate = mock(Crate.class);
        final UUID online = UUID.randomUUID();
        final UUID offline = UUID.randomUUID();
        final Path dataFile = directory.resolve("data.yml");
        final PausedYaml data = new PausedYaml();
        data.set("preserved", 42);
        java.nio.file.Files.writeString(dataFile, "preserved: 42\n");

        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("crate-test"));
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getCrateManager()).thenReturn(crateManager);
        when(plugin.isEnabled()).thenReturn(true);
        when(server.getAsyncScheduler()).thenReturn(scheduler);
        when(server.getPlayer(online)).thenReturn(player);
        when(player.getName()).thenReturn("OnlinePlayer");
        when(player.getUniqueId()).thenReturn(online);
        when(crateManager.getCrateFromName("Example")).thenReturn(crate);
        when(crate.getName()).thenReturn("Example");

        final BlockingQueue<Consumer<ScheduledTask>> tasks = new LinkedBlockingQueue<>();
        when(scheduler.runNow(eq(plugin), any())).thenAnswer(invocation -> {
            tasks.add(invocation.getArgument(1));
            return mock(ScheduledTask.class);
        });

        try (var singleton = mockStatic(CrazyCrates.class);
             var bukkit = mockStatic(Bukkit.class);
             var yaml = mockStatic(YamlConfiguration.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            singleton.when(CrazyCrates::get).thenReturn(plugin);
            bukkit.when(Bukkit::getAsyncScheduler).thenReturn(scheduler);
            yaml.when(() -> YamlConfiguration.loadConfiguration(dataFile.toFile())).thenReturn(data);
            final FileManager files = new FileManager();
            when(plugin.getFileManager()).thenReturn(files);
            files.setup();
            final BukkitUserManager users = new BukkitUserManager();

            users.addVirtualKeys(1, online, "Example");
            final Consumer<ScheduledTask> firstWrite = tasks.poll(5, TimeUnit.SECONDS);
            assertNotNull(firstWrite);

            try (var executor = Executors.newFixedThreadPool(5)) {
                final Future<?> saving = executor.submit(() -> firstWrite.accept(null));
                assertTrue(data.started.await(5, TimeUnit.SECONDS));
                try {
                    final var delivering = executor.submit(() -> {
                        try (var workerBukkit = mockStatic(Bukkit.class)) {
                            workerBukkit.when(Bukkit::getAsyncScheduler).thenReturn(scheduler);
                            assertTrue(users.addOfflineKeys(offline, "Example", 3, KeyType.physical_key));
                        }
                    });
                    try {
                        delivering.get(1, TimeUnit.SECONDS);
                    } catch (TimeoutException waitingForSnapshot) {
                        // The fixed writer holds the YAML lock until the snapshot is ready.
                    } finally {
                        data.resume.countDown();
                    }

                    // An unprotected writer throws when the first offline grant adds a top-level section.
                    saving.get(5, TimeUnit.SECONDS);
                    delivering.get(5, TimeUnit.SECONDS);
                    final List<Future<?>> grants = new ArrayList<>();
                    for (int worker = 0; worker < 4; worker++) {
                        grants.add(executor.submit(() -> {
                            for (int i = 0; i < 50; i++) {
                                users.addVirtualKeys(1, online, "Example");
                                assertTrue(users.addOfflineKeys(offline, "Example", 1, KeyType.physical_key));
                            }
                        }));
                    }
                    for (final Future<?> grant : grants) grant.get(5, TimeUnit.SECONDS);
                } finally {
                    data.resume.countDown();
                }
            }

            assertEquals(1, tasks.size(), "pending delivery writes are coalesced");
            tasks.remove().accept(null);
            assertTrue(tasks.isEmpty());

            final String onlinePath = "Players." + online + ".Example";
            final String offlinePath = "Offline-Players." + offline + ".Physical.Example";
            final YamlConfiguration reloaded = new YamlConfiguration();
            reloaded.load(dataFile.toFile());
            assertEquals(42, reloaded.getInt("preserved"));
            assertEquals(201, reloaded.getInt(onlinePath));
            assertEquals("OnlinePlayer", reloaded.getString("Players." + online + ".Name"));
            assertEquals(203, reloaded.getInt(offlinePath));

            // Force an I/O failure by temporarily blocking the destination with a directory.
            final Path priorFile = directory.resolve("prior-data.yml");
            java.nio.file.Files.move(dataFile, priorFile);
            java.nio.file.Files.createDirectory(dataFile);
            users.addVirtualKeys(1, online, "Example");
            tasks.remove().accept(null);
            assertTrue(java.nio.file.Files.isDirectory(dataFile));
            assertTrue(tasks.isEmpty(), "an I/O failure does not create a tight retry loop");
            java.nio.file.Files.delete(dataFile);
            java.nio.file.Files.move(priorFile, dataFile);

            when(plugin.isEnabled()).thenReturn(false);
            files.flushPendingSaves();
            files.flushPendingSaves();

            reloaded.load(dataFile.toFile());
            assertEquals(202, reloaded.getInt(onlinePath));
            assertEquals(203, reloaded.getInt(offlinePath));
            assertEquals(202, users.getVirtualKeys(online, "Example"));
        }
    }

    private static final class PausedYaml extends YamlConfiguration {
        private final AtomicBoolean first = new AtomicBoolean(true);
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch resume = new CountDownLatch(1);

        @Override
        public String saveToString() {
            if (first.getAndSet(false)) {
                final var sections = this.map.entrySet().iterator();
                started.countDown();
                try {
                    assertTrue(resume.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
                sections.next();
            }
            return super.saveToString();
        }
    }
}
