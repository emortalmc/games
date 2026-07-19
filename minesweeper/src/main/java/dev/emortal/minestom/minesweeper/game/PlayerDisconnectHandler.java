package dev.emortal.minestom.minesweeper.game;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerDisconnectHandler {

    private final @NotNull MinesweeperGame game;
    private final @NotNull TeamAllocator teamAllocator;

    public PlayerDisconnectHandler(@NotNull MinesweeperGame game, @NotNull TeamAllocator teamAllocator) {
        this.game = game;
        this.teamAllocator = teamAllocator;
    }

    public void onDisconnect(@NotNull Player left) {
        this.removeFromTeam(left);

        this.sendQuitMessage(left);
        this.playQuitSound();

        if (this.game.getPlayers().isEmpty()) {
            this.game.save();
            this.game.finish();
        }
    }

    private void removeFromTeam(@NotNull Player left) {
        this.teamAllocator.deallocate(left);
        left.removeTag(PlayerTags.COLOR);

        Team team = left.getTeam();
        if (team != null) MinecraftServer.getTeamManager().deleteTeam(team);
    }

    private void sendQuitMessage(@NotNull Player left) {
        Component message = Component.text()
                .append(Component.text("QUIT", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(left.getUsername(), NamedTextColor.RED))
                .build();
        this.game.sendMessage(message);
    }

    private void playQuitSound() {
        Sound sound = Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.MASTER, 1F, 0.5F);
        this.game.playSound(sound);
    }
}
