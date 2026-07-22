package dev.emortal.minestom.core.game.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public record GameCreationInfo(@NotNull String gameId, @Nullable String map, @NotNull Set<UUID> playerIds) {

}
