package dev.emortal.minestom.blocksumo.team;

import dev.emortal.minestom.blocksumo.game.PlayerTags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class PlayerTeamManager {

    private static final int STARTING_LIVES = 5;
    private final List<Integer> teamColours = new ArrayList<>();

    public void allocateTeam(@NotNull Player player) {
        int randomColour = -1;
        while (randomColour == -1 || teamColours.contains(randomColour)) { // Stop collisions
            randomColour = Color.HSBtoRGB(ThreadLocalRandom.current().nextFloat(), 1f, 1f);
        }

        teamColours.add(randomColour);

        player.setTag(PlayerTags.TEAM_COLOR, new TeamColor(TextColor.color(randomColour)));
    }

    public void setTeam(@NotNull Player player) {
        TeamColor allocatedColor = player.getTag(PlayerTags.TEAM_COLOR);

        Team minestomTeam = MinecraftServer.getTeamManager().createBuilder(allocatedColor.getTextColor().asHexString())
                .teamColor(allocatedColor.getTeamColor())
                .collisionRule(TeamsPacket.CollisionRule.NEVER)
                .updateTeamPacket()
                .build();

        this.updateTeamLives(minestomTeam, STARTING_LIVES);

        player.setTeam(minestomTeam);
    }

    public void updateTeamLives(@NotNull Player player, int lives) {
//        TeamColor allocatedColor = player.getTag(PlayerTags.TEAM_COLOR);
        updateTeamLives(player.getTeam(), lives);
    }

    public void updateTeamLives(@NotNull Team team, int lives) {
        String emptyFont = "\uE01D";
        String fullFont = "\uE01E";
        int emptyHearts = STARTING_LIVES - lives;

        TextComponent.Builder builder = Component.text()
                .append(Component.text(" • ", NamedTextColor.GRAY));

        for (int i = 0; i < lives; i++) {
            builder.append(Component.translatable("space.-1"));
            builder.append(Component.text(fullFont, NamedTextColor.WHITE).shadowColor(ShadowColor.none()));
        }
        for (int i = 0; i < emptyHearts; i++) {
            builder.append(Component.translatable("space.-1"));
            builder.append(Component.text(emptyFont, NamedTextColor.WHITE).shadowColor(ShadowColor.none()));
        }

        team.updateSuffix(builder.build());
    }
}
