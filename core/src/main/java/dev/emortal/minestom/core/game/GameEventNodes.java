package dev.emortal.minestom.core.game;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import org.jetbrains.annotations.NotNull;

final class GameEventNodes {
    // The event node used by the game manager to listen for events.
    static final @NotNull EventNode<Event> GAME_MANAGER = EventNode.all("game-manager");
    // The event node that is the parent of all the game event nodes, for an easy tree view when looking for game nodes.
    static final @NotNull EventNode<Event> GAMES = EventNode.all("games");
    static final @NotNull EventNode<Event> PRE_GAME = EventNode.all("pre-game");

    static {
        MinecraftServer.getGlobalEventHandler().addChild(GAME_MANAGER);
        MinecraftServer.getGlobalEventHandler().addChild(GAMES);
        GAME_MANAGER.addChild(PRE_GAME);
    }

    private GameEventNodes() {
    }
}
