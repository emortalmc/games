package dev.emortal.minestom.core.game;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface GameProvider {

    @Nullable Game findGame(@NotNull Player player);

    int getGameCount();
}
