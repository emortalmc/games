package dev.emortal.minestom.minesweeper.game;

import dev.emortal.minestom.minesweeper.util.TeamColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class TeamAllocator {
    private static final List<TeamColor> AVAILABLE_COLORS = List.of(TeamColor.values());

    private final Queue<TeamColor> availableColors = new ArrayDeque<>(AVAILABLE_COLORS);

    public void allocate(@NotNull Player player) {
        TeamColor color = this.availableColors.poll();
        if (color == null) {
            color = getRandom();
        }

        Team team = MinecraftServer.getTeamManager().createBuilder(UUID.randomUUID().toString()).teamColor(color.teamColor()).build();
        player.setTeam(team);
        player.setTag(PlayerTags.COLOR, color);
    }

    public void deallocate(@NotNull Player player) {
        TeamColor color = player.getTag(PlayerTags.COLOR);
        if (color == null) return;

        this.availableColors.add(color);
    }

    private static @NotNull TeamColor getRandom() {
        return AVAILABLE_COLORS.get(ThreadLocalRandom.current().nextInt(AVAILABLE_COLORS.size()));
    }
}
