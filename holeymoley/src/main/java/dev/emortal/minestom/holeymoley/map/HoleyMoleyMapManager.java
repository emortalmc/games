package dev.emortal.minestom.holeymoley.map;

import dev.emortal.minestom.core.map.LoadedMap;
import dev.emortal.minestom.core.map.MapManager;
import io.github.togar2.pvp.feature.CombatFeatures;
import io.github.togar2.pvp.feature.FeatureType;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.color.Color;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.attribute.EnvironmentAttribute;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class HoleyMoleyMapManager implements MapManager {

    private final int radius;
    private final RegistryKey<DimensionType> dimension;

    public HoleyMoleyMapManager(int radius) {
        this.radius = radius;

        DimensionType overworld = MinecraftServer.getDimensionTypeRegistry().get(DimensionType.OVERWORLD);
        DimensionType dimensionType = DimensionType.builder()
                .skylight(true)
                .ambientLight(1f)
                .setAttribute(EnvironmentAttribute.AMBIENT_LIGHT_COLOR, Color.WHITE)
                .defaultClock(overworld.defaultClock())
                .timelines(overworld.timelines())
                .build();
        this.dimension = MinecraftServer.getDimensionTypeRegistry().register(Key.key("emortalmc:holeymoley"), dimensionType);
    }

    @Override
    public @NotNull LoadedMap loadMap(@NotNull String mapName) {
        return loadRandomMap();
    }

    @Override
    public @NotNull LoadedMap loadRandomMap() {
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer(this.dimension);
        instance.setTime(0);
        instance.defaultClock().pause();
        instance.setExplosionSupplier(CombatFeatures.modernVanilla().get(FeatureType.EXPLOSION).getExplosionSupplier());

        // Generate a big cube of dirt surrounded by bedrock
        int radius = this.radius;
        instance.setGenerator(unit -> unit.modifier().setAll((x, y, z) -> {
            if (y < -radius || y > radius) return Block.AIR;
            if (x < -radius || x > radius) return Block.AIR;
            if (z < -radius || z > radius) return Block.AIR;

            if (y == -radius || y == radius) return Block.BEDROCK;
            if (x == -radius || x == radius) return Block.BEDROCK;
            if (z == -radius || z == radius) return Block.BEDROCK;

            return Block.DIRT;
        }));

        return new LoadedMap(instance, null);
    }

    @Override
    public @NotNull Set<String> getMaps() {
        return Set.of();
    }
}
