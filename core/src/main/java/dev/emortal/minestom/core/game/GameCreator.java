package dev.emortal.minestom.core.game;

import dev.emortal.minestom.core.game.config.GameCreationInfo;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface GameCreator {

    @NotNull Game createGame(@NotNull GameCreationInfo info);
}
