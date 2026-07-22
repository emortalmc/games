package dev.emortal.minestom.core.utils;

import net.hollowcube.polar.PolarLoader;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class PolarUtil {

    public static CompletableFuture<Void> stream(InstanceContainer instance, Path path) {
        try {
            File file = path.toFile();
            CompletableFuture<Void> future = PolarLoader.streamLoad(instance, FileChannel.open(path), file.length(), null, null, true);
            future.exceptionally(a -> {
                a.printStackTrace();
                return null;
            });
            return future.thenRun(() -> {
                loadSurrounding(instance);
            });
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static void loadSurrounding(Instance instance) {
        for (Chunk chunk : instance.getChunks()) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    instance.loadChunk(chunk.getChunkX() + x, chunk.getChunkZ() + z);
                }
            }
        }
    }

}
