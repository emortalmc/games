package dev.emortal.minestom.core.game;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.Event;
import org.jetbrains.annotations.NotNull;

public record GameUpdateRequestEvent(@NotNull Game game) implements Event {

    public static void trigger(@NotNull Game game) {
        MinecraftServer.getGlobalEventHandler().call(new GameUpdateRequestEvent(game));
    }
}
