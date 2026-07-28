package dev.emortal.minestom.holeymoley.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.color.TeamColor;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

public final class PlayerTeams {
    public static final @NotNull Team ALIVE = MinecraftServer.getTeamManager().createBuilder("holeymoley_alive")
            .teamColor(TeamColor.RED)
            .nameTagVisibility(TeamsPacket.NameTagVisibility.ALWAYS)
            .updateTeamPacket()
            .build();
    public static final @NotNull Team DEAD = MinecraftServer.getTeamManager().createBuilder("holeymoley_dead")
            .teamColor(TeamColor.GRAY)
            .prefix(Component.text("☠ ", NamedTextColor.GRAY))
            .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
            .updateTeamPacket()
            .build();

    private PlayerTeams() {
    }
}
