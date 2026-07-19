package dev.emortal.minestom.marathon.command;

import dev.emortal.minestom.core.utils.DurationFormatter;
import dev.emortal.minestom.marathon.leaderboard.LeaderboardDB;
import dev.emortal.minestom.marathon.leaderboard.LeaderboardEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.condition.Conditions;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public final class LeaderboardCommand extends Command {

    private final LeaderboardDB db;
    public LeaderboardCommand(LeaderboardDB db) {
        super("leaderboard");
        this.db = db;

        super.setCondition(Conditions::playerOnly);
        super.setDefaultExecutor(this::execute);
    }

    private void execute(@NotNull CommandSender sender, @NotNull CommandContext context) {
        CompletableFuture.runAsync(() -> {
            List<LeaderboardEntry> entries = db.getTopScores(10);

            TextComponent.Builder component = Component.text();

            component.append(MiniMessage.miniMessage().deserialize("<gradient:#d900ff:#ff00b3><bold>     Marathon Leaderboard"));

            int i = 0;
            for (LeaderboardEntry entry : entries) {
                appendLeaderboardEntry(++i, entry, component);
            }

            sender.sendMessage(component.build());
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    private void appendLeaderboardEntry(int i, LeaderboardEntry entry, TextComponent.Builder builder) {
        Style color = switch (i) {
            case 1 -> Style.style(NamedTextColor.GOLD, TextDecoration.BOLD);
            case 2 -> Style.style(TextColor.color(210, 210, 210), TextDecoration.BOLD);
            case 3 -> Style.style(TextColor.color(205, 127, 50), TextDecoration.BOLD);
            default -> Style.style(TextColor.color(140, 140, 140));
        };
        Style nameColor = switch (i) {
            case 1 -> Style.style(NamedTextColor.GOLD);
            case 2 -> Style.style(TextColor.color(210, 210, 210));
            case 3 -> Style.style(TextColor.color(205, 127, 50));
            default -> Style.style(TextColor.color(140, 140, 140));
        };
        Style scoreColor = switch (i) {
            case 1,2,3 -> Style.style(NamedTextColor.LIGHT_PURPLE);
            default -> Style.style(TextColor.color(0x006c96));
        };
        Style timesColor = switch (i) {
            case 1,2,3 -> Style.style(NamedTextColor.GRAY);
            default -> Style.style(TextColor.color(110, 110, 110));
        };


        String formattedTimeTaken = DurationFormatter.formatDuration(Duration.ofMillis(entry.ticks() * 50L));
        if (formattedTimeTaken.isBlank()) formattedTimeTaken = "0s";

        double bps = ((double)entry.score() / (entry.ticks() * 50L)) * 1000;

        builder.append(
                Component.text()
                        .append(Component.text("\n #", color).decoration(TextDecoration.BOLD, false))
                        .append(Component.text(i, color))
                        .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(entry.name(), nameColor))
                        .append(Component.space())
                        .append(Component.text(entry.score(), scoreColor))
                        .append(Component.text(" (", timesColor))
                        .append(Component.text(formattedTimeTaken, timesColor))
                        .append(Component.text(" %.2f".formatted(bps), timesColor)) // 2 d.p
                        .append(Component.text("bps)", timesColor))
        );
    }

}
