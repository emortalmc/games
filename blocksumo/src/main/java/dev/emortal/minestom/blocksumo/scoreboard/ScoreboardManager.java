package dev.emortal.minestom.blocksumo.scoreboard;

import dev.emortal.minestom.blocksumo.game.BlockSumoGame;
import dev.emortal.minestom.blocksumo.game.PlayerTags;
import dev.emortal.minestom.blocksumo.utils.text.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.Viewable;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Sidebar;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

public final class ScoreboardManager implements Viewable {

    private static final @NotNull Component FOOTER = Component.text()
            .append(Component.text(TextUtil.smallFont("mc.emortal.dev"), NamedTextColor.DARK_GRAY))
            .build();

    // max lines is 12 because we take up 3 of our 15 lines with the header and footer
    private static final int MAX_LINES = 12;

    private final @NotNull Sidebar sidebar = new Sidebar(BlockSumoGame.TITLE);

    private Set<Player> scores = Set.of();

    public ScoreboardManager() {

        this.sidebar.createLine(new Sidebar.ScoreboardLine("header_spacer", Component.empty(), 99));
        this.sidebar.createLine(new Sidebar.ScoreboardLine("footer_spacer", Component.empty(), -8));
        this.sidebar.createLine(new Sidebar.ScoreboardLine("footer", FOOTER, -9));
    }

    public void updateScoreboard() {
        Set<Player> newScores = this.getViewers().stream()
                .sorted(Comparator.comparingInt(p -> -p.getTag(PlayerTags.LIVES)))
                .limit(MAX_LINES)
                .collect(Collectors.toSet());

        // remove any players that are no longer within the top MAX_LINES
        for (Player p : this.scores) {
            if (newScores.contains(p)) continue;
            this.sidebar.removeLine(p.getUuid().toString());
        }

        for (Player score : this.scores) {
            updateLine(score);
        }

        this.scores = newScores;
    }

    private void updateLine(@NotNull Player player) {
        byte lives = player.getTag(PlayerTags.LIVES);

        String lineId = player.getUuid().toString();

        if (this.sidebar.getLine(lineId) != null) { // line already exists, update
            this.sidebar.updateLineContent(lineId, createScoreboardComponent(player, lives));
            this.sidebar.updateLineScore(lineId, lives);
        } else { // line does not exist, create
            this.sidebar.createLine(new Sidebar.ScoreboardLine(lineId, createScoreboardComponent(player, lives), lives));
        }
    }

    private @NotNull Component createScoreboardComponent(@NotNull Player player, byte lives) {
        TextColor livesColor;
        if (lives == 5) {
            livesColor = NamedTextColor.GREEN;
        } else {
            livesColor = TextColor.lerp((lives - 1) / 4F, NamedTextColor.RED, NamedTextColor.GREEN);
        }
        if (lives > 5) {
            livesColor = NamedTextColor.LIGHT_PURPLE;
        }

        if (!player.hasTag(PlayerTags.TEAM_COLOR)) throw new IllegalStateException("Player " + player.getUsername() + " has no team!");

        return Component.text()
                .append(Component.text(player.getUsername(), player.getTag(PlayerTags.TEAM_COLOR).getTextColor()))
                .append(Component.text(" - ", NamedTextColor.GRAY))
                .append(Component.text(lives, livesColor, TextDecoration.BOLD))
                .build();
    }

    @Override
    public boolean addViewer(@NotNull Player player) {
        return this.sidebar.addViewer(player);
    }

    @Override
    public boolean removeViewer(@NotNull Player player) {
        return this.sidebar.removeViewer(player);
    }

    @Override
    public @NotNull Set<? extends Player> getViewers() {
        return this.sidebar.getViewers();
    }

}
