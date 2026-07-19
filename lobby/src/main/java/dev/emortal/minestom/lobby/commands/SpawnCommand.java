package dev.emortal.minestom.lobby.commands;

import dev.emortal.minestom.lobby.Entrypoint;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand extends Command {

    public SpawnCommand(@NotNull Instance lobbyInstance) {
        super("spawn");

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) return;

            if (player.getInstance().getUuid().equals(lobbyInstance.getUuid())) {
                player.teleport(Entrypoint.SPAWN_POINT);
            } else {
                player.setInstance(lobbyInstance, Entrypoint.SPAWN_POINT);
            }
        });
    }

}
