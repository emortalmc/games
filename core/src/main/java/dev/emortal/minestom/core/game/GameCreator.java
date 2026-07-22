package dev.emortal.minestom.core.game;

import dev.emortal.minestom.core.game.config.GameCreationInfo;
import dev.emortal.minestom.core.map.LoadedMap;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface GameCreator {

    @NotNull Game createGame(@NotNull GameCreationInfo info, @NotNull LoadedMap map);
}
