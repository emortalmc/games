package dev.emortal.minestom.core.utils;

import net.hollowcube.polar.PolarWorldAccess;
import net.minestom.server.MinecraftServer;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.NotNull;

public class FixedPolarWorldAccess implements PolarWorldAccess {
    @Override
    public int getBiomeId(@NotNull String name) {
        var biomeRegistry = MinecraftServer.getBiomeRegistry();
        return biomeRegistry.getId(RegistryKey.of(name));
    }
}
