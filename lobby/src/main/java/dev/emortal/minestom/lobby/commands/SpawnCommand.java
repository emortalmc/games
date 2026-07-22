package dev.emortal.minestom.lobby.commands;

import dev.emortal.minestom.lobby.LobbyModule;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.SoundStop;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand extends Command {

    public SpawnCommand(@NotNull Instance lobbyInstance) {
        super("spawn");

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) return;

            player.stopSound(SoundStop.named(Key.key("song.abs.clear")));
            player.stopSound(SoundStop.named(Key.key("song.abs.muffled")));

            if (player.getInstance().getUuid().equals(lobbyInstance.getUuid())) {
                player.teleport(LobbyModule.SPAWN_POINT);
            } else {
                player.setInstance(lobbyInstance, LobbyModule.SPAWN_POINT);
            }
        });
    }

}
