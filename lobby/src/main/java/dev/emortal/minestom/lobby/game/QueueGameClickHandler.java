package dev.emortal.minestom.lobby.game;

import dev.emortal.messaging.message.Channel;
import dev.emortal.messaging.message.MatchmakeMessage;
import dev.emortal.minestom.core.EmortalServer;
import dev.emortal.minestom.lobby.config.GameModeConfig;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class QueueGameClickHandler {
    static void leftClick(@NotNull Player player, @NotNull GameModeConfig config) {
        EmortalServer.getRedis().sendMessage(Channel.PROXY, new MatchmakeMessage(config.id(), List.of(player.getUuid())));
    }

    static void rightClick(@NotNull Player player, @NotNull GameModeConfig config) {
        if (config.maps() == null || config.maps().isEmpty()) {
            leftClick(player, config);
            return;
        }

        // actual right click logic
        MapSelectorInventory mapInventory = new MapSelectorInventory(config, false);
        player.openInventory(mapInventory);
    }

    private QueueGameClickHandler() {
    }
}
