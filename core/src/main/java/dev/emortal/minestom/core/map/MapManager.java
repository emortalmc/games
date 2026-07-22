package dev.emortal.minestom.core.map;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface MapManager {

    @NotNull LoadedMap loadMap(@NotNull String mapName);

    @NotNull LoadedMap loadRandomMap();

    @NotNull Set<String> getMaps();

}
