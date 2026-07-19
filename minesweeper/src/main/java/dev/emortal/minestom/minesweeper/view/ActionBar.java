package dev.emortal.minestom.minesweeper.view;

import dev.emortal.minestom.minesweeper.board.Board;
import dev.emortal.minestom.minesweeper.map.MapManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public final class ActionBar {

    private final @NotNull Board board;
    private final long startTime;

    private int flags;

    public ActionBar(@NotNull Board board) {
        this.board = board;
        this.startTime = System.currentTimeMillis() - board.getStartingTicks() * 50L;

        // Keep action bar shown
        this.board.getInstance().scheduler().buildTask(this::update).repeat(TaskSchedule.tick(20)).schedule();
    }

    public void incrementLives() {
        if (this.board.getLives() < MapManager.MAX_LIVES) {
            this.board.setLives(this.board.getLives() + 1);
            this.update();
        }
    }

    public int decrementLives() {
        this.board.setLives(this.board.getLives() - 1);
        this.update();
        return this.board.getLives();
    }

    public void incrementFlags() {
        this.flags++;
        this.update();
    }

    public void decrementFlags() {
        if (this.flags > 0) this.flags--;
        this.update();
    }

    public void update() {
        long now = System.currentTimeMillis();
        Duration duration = Duration.ofMillis(now - this.startTime);

        // ☠ {mines} MINES | ⚑ {flags} FLAGS | ⌚ 1m 23s
        this.board.getInstance().sendActionBar(Component.text().append(Component.text("⚑ ", NamedTextColor.GREEN))
                .append(Component.text(this.flags, NamedTextColor.GREEN))
                .append(Component.text(" FLAGS", NamedTextColor.GREEN))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("♥ ", NamedTextColor.RED)).append(Component.text(this.board.getLives(), NamedTextColor.RED))
                .append(Component.text(" LIVES", NamedTextColor.RED))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("⌚ ", NamedTextColor.AQUA))
                .append(Component.text(this.formatDuration(duration), NamedTextColor.AQUA)));
    }

    private @NotNull String formatDuration(@NotNull Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (hours == 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        }
    }
}
