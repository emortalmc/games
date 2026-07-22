package dev.emortal.minestom.marathon;

import dev.emortal.minestom.core.map.LoadedMap;
import dev.emortal.minestom.core.map.MapManager;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class MarathonMapManager implements MapManager {

    @Override
    public @NotNull LoadedMap loadMap(@NotNull String mapName) {
        return loadRandomMap();
    }

    @Override
    public @NotNull LoadedMap loadRandomMap() {
        return new LoadedMap(null, null);
    }

    @Override
    public @NotNull Set<String> getMaps() {
        return Set.of();
    }
}
