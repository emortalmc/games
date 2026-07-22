package dev.emortal.minestom.lobby;

import com.alibaba.fastjson2.JSONObject;
import dev.emortal.minestom.core.map.LoadedMap;
import dev.emortal.minestom.core.map.MapManager;
import net.minestom.server.instance.InstanceContainer;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class LobbyMapManager implements MapManager {
    private final LoadedMap map;
    public LobbyMapManager(InstanceContainer instance) {
        JSONObject jsonObject = new JSONObject();

        this.map = new LoadedMap(instance, jsonObject);
    }

    public InstanceContainer getInstance() {
        return map.instance();
    }

    @Override
    public @NotNull LoadedMap loadMap(@NotNull String mapName) {
        return map;
    }

    @Override
    public @NotNull LoadedMap loadRandomMap() {
        return map;
    }

    @Override
    public @NotNull Set<String> getMaps() {
        return Set.of();
    }
}
