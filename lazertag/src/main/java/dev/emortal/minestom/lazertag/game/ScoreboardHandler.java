package dev.emortal.minestom.lazertag.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Sidebar;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.function.Consumer;

public final class ScoreboardHandler {
    private static final Comparator<Player> PLAYER_COMPARATOR = Comparator.comparingInt(player -> {
        Integer kills = player.getTag(DamageHandler.KILLS_TAG);
        return kills == null ? 0 : kills;
    });

    private final @NotNull LazerTagGame game;
    private final @NotNull Sidebar sidebar;

    public ScoreboardHandler(@NotNull LazerTagGame game) {
        this.game = game;
        this.sidebar = new Sidebar(Component.text("☠", NamedTextColor.RED));

        int lines = 8;
        for (int i = 0; i < lines; i++) {
            this.sidebar.createLine(new Sidebar.ScoreboardLine("kills" + i, Component.empty(), lines - i, Sidebar.NumberFormat.blank()));
        }

        this.sidebar.updateLineContent("kills0", Component.text("Be the first to kill!", NamedTextColor.GOLD));
    }

    public void show(@NotNull Player player) {
        this.sidebar.addViewer(player);
    }

    public void hide(@NotNull Player player) {
        this.sidebar.removeViewer(player);
    }

    public void refreshScoreboard() {
        this.game.getPlayers().stream().sorted(PLAYER_COMPARATOR.reversed()).forEachOrdered(new RefreshAction());
    }

    private final class RefreshAction implements Consumer<Player> {

        private int i = 0;

        @Override
        public void accept(@NotNull Player player) {
            Integer kills = player.getTag(DamageHandler.KILLS_TAG);
            if (kills == null) return;

            Style color = switch (this.i + 1) {
                case 1 -> Style.style(NamedTextColor.GOLD, TextDecoration.BOLD);
                case 2 -> Style.style(TextColor.color(210, 210, 210), TextDecoration.BOLD);
                case 3 -> Style.style(TextColor.color(205, 127, 50), TextDecoration.BOLD);
                default -> Style.style(TextColor.color(140, 140, 140));
            };
            Style nameColor = switch (this.i + 1) {
                case 1 -> Style.style(NamedTextColor.GOLD);
                case 2 -> Style.style(TextColor.color(210, 210, 210));
                case 3 -> Style.style(TextColor.color(205, 127, 50));
                default -> Style.style(TextColor.color(140, 140, 140));
            };
            Style scoreColor = switch (this.i + 1) {
                case 1,2,3 -> Style.style(NamedTextColor.LIGHT_PURPLE);
                default -> Style.style(TextColor.fromHexString("#006c96"));
            };

            ScoreboardHandler.this.sidebar.updateLineContent(
                    "kills" + this.i,
                    Component.text()
                            .append(Component.text(this.i + 1 + ". ", color))
                            .append(Component.text(player.getUsername(), nameColor))
                            .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                            .append(Component.text(kills, scoreColor))
                            .build()
            );
            this.i++;
        }
    }
}
