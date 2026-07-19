package dev.emortal.minestom.lobby.util;

import net.hollowcube.polar.PolarLoader;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceContainer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class PolarConvertingLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolarConvertingLoader.class);

    private final @NotNull InstanceContainer instance;
    private final @NotNull String path;

    public PolarConvertingLoader(@NotNull String path) {
        this(MinecraftServer.getInstanceManager().createInstanceContainer(), path);
    }

    public PolarConvertingLoader(@NotNull InstanceContainer instance, @NotNull String path) {
        this.instance = instance;
        this.path = path;
    }

    public @NotNull CompletableFuture<InstanceContainer> load() {
        Path polarFile = Path.of(this.path + ".polar");

        CompletableFuture<Void> loader = this.loadFromPolar(polarFile);;

        return loader.thenApply(ignored -> this.instance);
    }

    private @NotNull CompletableFuture<Void> loadFromPolar(@NotNull Path file) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            return PolarLoader.streamLoad(this.instance, Channels.newChannel(new ByteArrayInputStream(bytes)), bytes.length, null, null, true);
        } catch (IOException exception) {
            LOGGER.error("Failed to load polar world from '{}'", file);
            throw new UncheckedIOException(exception);
        }
    }
}
