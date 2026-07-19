package dev.emortal.minestom.blocksumo.game;

import dev.emortal.minestom.blocksumo.Main;
import dev.emortal.minestom.blocksumo.spawning.PlayerRespawnHandler;
import dev.emortal.minestom.blocksumo.spawning.SpawnProtectionManager;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public final class PlayerDisconnectHandler {

    private final @NotNull BlockSumoGame game;
    private final @NotNull PlayerManager playerManager;
    private final @NotNull PlayerRespawnHandler respawnHandler;
    private final @NotNull SpawnProtectionManager spawnProtectionManager;

    public PlayerDisconnectHandler(@NotNull BlockSumoGame game, @NotNull PlayerManager playerManager, @NotNull PlayerRespawnHandler respawnHandler,
                                   @NotNull SpawnProtectionManager spawnProtectionManager) {
        this.game = game;
        this.playerManager = playerManager;
        this.respawnHandler = respawnHandler;
        this.spawnProtectionManager = spawnProtectionManager;
    }

    public void onDisconnect(@NotNull Player player) {
        this.playerManager.cleanUpPlayer(player);

        this.sendQuitMessage(player);
        this.playQuitSound();

        this.cancelCountdownIfNeeded();
        boolean singlePlayerWinner = this.hasSinglePlayerWinner();
        if (singlePlayerWinner) this.endWithSinglePlayerWinner();

        this.respawnHandler.cleanUpPlayer(player);

        this.playerManager.cleanUpPlayer(player);
        if (!singlePlayerWinner) this.checkForWinner();
    }

    private void sendQuitMessage(@NotNull Player left) {
        Component message = Component.text()
                .append(Component.text("QUIT", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(left.getUsername(), NamedTextColor.RED))
                .append(Component.text(" left the game", NamedTextColor.GRAY))
                .build();
        this.game.sendMessage(message);
    }

    private void playQuitSound() {
        Sound sound = Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.MASTER, 1F, 0.5F);
        this.game.playSound(sound);
    }

    private void cancelCountdownIfNeeded() {
        if (this.game.getPlayers().size() >= Main.MIN_PLAYERS) return;
        this.game.cancelCountdown();
    }

    private boolean hasSinglePlayerWinner() {
        return this.game.getPlayers().size() == 1;
    }

    private void endWithSinglePlayerWinner() {
        Player winner = this.game.getPlayers().iterator().next();
        this.game.victory(Set.of(winner));
    }

    private void checkForWinner() {
        Set<Player> alivePlayers = new HashSet<>();
        for (Player player : this.game.getPlayers()) {
            if (player.getTag(PlayerTags.LIVES) > 0) alivePlayers.add(player);
        }
        if (alivePlayers.isEmpty()) return;

        Player firstPlayer = alivePlayers.iterator().next();
        for (Player alive : alivePlayers) {
            if (!alive.getTag(PlayerTags.TEAM_COLOR).equals(firstPlayer.getTag(PlayerTags.TEAM_COLOR))) return;
        }
        this.game.victory(alivePlayers);
    }
}
