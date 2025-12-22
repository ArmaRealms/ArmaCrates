package com.badbones69.crazycrates.api;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.enums.Messages;
import com.badbones69.crazycrates.api.events.PlayerPrizeEvent;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.Prize;
import com.badbones69.crazycrates.api.objects.Tier;
import com.badbones69.crazycrates.api.objects.other.ItemBuilder;
import com.badbones69.crazycrates.api.utils.MiscUtils;
import com.badbones69.crazycrates.api.utils.MsgUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import static com.badbones69.crazycrates.api.utils.MiscUtils.RANDOM;
import static java.util.regex.Matcher.quoteReplacement;

public class PrizeManager {

    @NotNull
    private static final CrazyCrates plugin = CrazyCrates.get();

    /**
     * Gets the prize for the player.
     *
     * @param player who the prize is for.
     * @param crate  the player is opening.
     * @param prize  the player is being given.
     */
    public static void givePrize(final Player player, Prize prize, final Crate crate) {
        if (prize == null) {
            plugin.debug(() -> "No prize was found when giving " + player.getName() + " a prize.", Level.WARNING);
            return;
        }

        prize = prize.hasPermission(player) ? prize.getAlternativePrize() : prize;

        for (final ItemStack item : prize.getItems()) {
            if (item == null) {
                final HashMap<String, String> placeholders = new HashMap<>();
                placeholders.put("%crate%", prize.getCrateName());
                placeholders.put("%prize%", prize.getPrizeName());
                player.sendMessage(Messages.prize_error.getMessage(placeholders, player));
                continue;
            }

            if (!MiscUtils.isInventoryFull(player)) {
                player.getInventory().addItem(item);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }

        for (final ItemBuilder item : prize.getItemBuilders()) {
            final ItemBuilder clone = new ItemBuilder(item).setTarget(player);

            if (!MiscUtils.isInventoryFull(player)) {
                player.getInventory().addItem(clone.build());
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), clone.build());
            }
        }

        for (String command : prize.getCommands()) { // /give %player% iron %random%:1-64
            if (command.contains("%random%:")) {
                final String cmd = command;
                final StringBuilder commandBuilder = new StringBuilder();

                for (String word : cmd.split(" ")) {
                    if (word.startsWith("%random%:")) {
                        word = word.replace("%random%:", "");

                        try {
                            final long min = Long.parseLong(word.split("-")[0]);
                            final long max = Long.parseLong(word.split("-")[1]);
                            commandBuilder.append(MiscUtils.pickNumber(min, max)).append(" ");
                        } catch (final Exception e) {
                            commandBuilder.append("1 ");

                            plugin.getLogger().warning("The prize " + prize.getPrizeName() + " in the " + prize.getCrateName() + " crate has caused an error when trying to run a command.");
                            plugin.getLogger().warning("Command: " + cmd);
                        }
                    } else {
                        commandBuilder.append(word).append(" ");
                    }
                }

                command = commandBuilder.toString();
                command = command.substring(0, command.length() - 1);
            }

            if (MiscUtils.isPapiActive()) command = PlaceholderAPI.setPlaceholders(player, command);

            final String display = prize.getDisplayItemBuilder().getName();

            final String name = display == null || display.isEmpty() ? MsgUtils.color(prize.getDisplayItemBuilder().getMaterial().getKey().getKey().toUpperCase(Locale.ROOT).replaceAll("_", " ")) : display;

            MiscUtils.sendCommand(command
                    .replaceAll("%player%", quoteReplacement(player.getName()))
                    .replaceAll("%Player%", quoteReplacement(player.getName()))
                    .replaceAll("%reward%", quoteReplacement(name))
                    .replaceAll("%crate%", quoteReplacement(crate.getCrateInventoryName())));
        }

        if (!crate.getPrizeMessage().isEmpty() && prize.getMessages().isEmpty()) {
            for (final String message : crate.getPrizeMessage()) {
                sendMessage(player, prize, crate, message);
            }

            return;
        }

        for (final String message : prize.getMessages()) {
            sendMessage(player, prize, crate, message);
        }
    }

    private static void sendMessage(final Player player, final Prize prize, final Crate crate, final String message) {
        final String display = prize.getDisplayItemBuilder().getName();

        final String name = display == null || display.isEmpty() ? MsgUtils.color(prize.getDisplayItemBuilder().getMaterial().getKey().getKey().toUpperCase(Locale.ROOT).replaceAll("_", " ")) : display;

        final String defaultMessage = message
                .replaceAll("%player%", quoteReplacement(player.getName()))
                .replaceAll("%Player%", quoteReplacement(player.getName()))
                .replaceAll("%reward%", quoteReplacement(name))
                .replaceAll("%crate%", quoteReplacement(crate.getCrateInventoryName()));

        MsgUtils.sendMessage(player, MiscUtils.isPapiActive() ? PlaceholderAPI.setPlaceholders(player, defaultMessage) : defaultMessage, false);
    }

    /**
     * Gets the prize for the player.
     *
     * @param player who the prize is for.
     * @param crate  the player is opening.
     * @param prize  the player is being given.
     */
    public static void givePrizeAndCallEvent(final Player player, final Crate crate, final Prize prize) {
        if (prize != null) {
            givePrize(player, prize, crate);

            if (prize.useFireworks()) MiscUtils.spawnFirework(player.getLocation().add(0, 1, 0), null);

            plugin.getServer().getPluginManager().callEvent(new PlayerPrizeEvent(player, crate, crate.getName(), prize));
        } else {
            player.sendMessage(MsgUtils.getPrefix("&cNo prize was found, please report this issue if you think this is an error."));
        }
    }

    public static void getPrize(final Crate crate, final Inventory inventory, final int slot, final Player player) {
        final ItemStack item = inventory.getItem(slot);

        if (item == null) return;

        final Prize prize = crate.getPrize(item);

        givePrize(player, prize, crate);
    }

    public static Tier getTier(final Crate crate) {
        if (crate.getTiers() != null && !crate.getTiers().isEmpty()) {
            for (int stopLoop = 0; stopLoop <= 100; stopLoop++) {
                for (final Tier tier : crate.getTiers()) {
                    final int chance = tier.getChance();

                    final int num = MiscUtils.useOtherRandom() ? ThreadLocalRandom.current().nextInt(tier.getMaxRange()) : RANDOM.nextInt(tier.getMaxRange());

                    if (num >= 1 && num <= chance) {
                        return tier;
                    }
                }
            }
        }

        return null;
    }
}