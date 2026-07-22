package dev.emortal.minestom.core.map;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import dev.emortal.minestom.core.utils.PolarUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.color.Color;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.attribute.EnvironmentAttribute;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class MapManagerImpl implements MapManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapManagerImpl.class);

    public static final int CHUNK_LOADING_RADIUS = 10;

    private static final Path MAPS_PATH = Path.of("maps");

    private final String gameId;
    private final Set<String> enabledMaps;
    private final RegistryKey<DimensionType> dimKey = DimensionType.OVERWORLD;

    public MapManagerImpl(String gameId, Set<String> enabledMaps) {
        this.gameId = gameId;
        this.enabledMaps = enabledMaps;

        DimensionType overworld = MinecraftServer.getDimensionTypeRegistry().get(DimensionType.OVERWORLD);

        DimensionType dimensionTypeFB = DimensionType.builder()
                .timelines(overworld.timelines())
                .setAttribute(EnvironmentAttribute.CLOUD_COLOR, ShadowColor.fromHexString("#ffffffcc"))
                .setAttribute(EnvironmentAttribute.FOG_COLOR, new Color(0xc0d8ff))
                .setAttribute(EnvironmentAttribute.SKY_COLOR, new Color(0x78a7ff))
                .setAttribute(EnvironmentAttribute.CLOUD_HEIGHT, 110f)
                .setAttribute(EnvironmentAttribute.AMBIENT_LIGHT_COLOR, NamedTextColor.WHITE)
                .defaultClock(overworld.defaultClock())
                .ambientLight(1f)
                .build();
        MinecraftServer.getDimensionTypeRegistry().register("emortalmc:fullbright", dimensionTypeFB);
    }

    public @NotNull LoadedMap loadMap(@NotNull String mapName) {
        if (!enabledMaps.contains(mapName)) {
            LOGGER.warn("Map '{}' does not exist or is not enabled, picking random instead", mapName);
            List<String> maps = new ArrayList<>(enabledMaps);
            mapName = maps.get(ThreadLocalRandom.current().nextInt(enabledMaps.size()));
        }

        Path gameMapsPath = MAPS_PATH.resolve(gameId);
        Path mapPath = gameMapsPath.resolve(mapName);
        Path polarPath = mapPath.resolve("map.polar");
        Path dataPath = mapPath.resolve("data.json");

        try {
            byte[] mapDataBytes = Files.readAllBytes(dataPath);
            JSONObject jsonObject = JSON.parseObject(mapDataBytes);
            LOGGER.info("Loaded data for map {}: [{}]", mapName, jsonObject);

            InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer(dimKey);
            instance.setTime(0);
            instance.defaultClock().pause();

            instance.enableAutoChunkLoad(false);
            PolarUtil.stream(instance, polarPath).join();

            return new LoadedMap(instance, jsonObject);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public @NotNull LoadedMap loadRandomMap() {
        List<String> maps = new ArrayList<>(enabledMaps);
        String mapName = maps.get(ThreadLocalRandom.current().nextInt(enabledMaps.size()));
        return loadMap(mapName);
    }

    @Override
    public @NotNull Set<String> getMaps() {
        return enabledMaps;
    }
}