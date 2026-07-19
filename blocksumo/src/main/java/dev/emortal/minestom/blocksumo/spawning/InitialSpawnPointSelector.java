package dev.emortal.minestom.blocksumo.spawning;

import dev.emortal.minestom.blocksumo.game.BlockSumoGame;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Queue;

public final class InitialSpawnPointSelector {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitialSpawnPointSelector.class);
    private static final double TWO_PI = Math.PI * 2;

    private final Queue<Pos> spawns;

    public InitialSpawnPointSelector(int playerCount, int spawnRadius) {
        this.spawns = this.generateSpawns(playerCount, spawnRadius);
    }

    private @NotNull Queue<Pos> generateSpawns(int playerCount, int spawnRadius) {
        LOGGER.info("Generating spawn points for {} players", playerCount); // Bug: players spawning in middle of map

        double playerOffset = TWO_PI / Math.max(playerCount, 1); // Fixes 0 player count on local testing

        Queue<Pos> spawns = new ArrayDeque<>();
        for (int i = 0; i <= playerCount; i++) {
            Pos pos = floorPos(new Pos(Math.cos(playerOffset * i) * spawnRadius, 0, Math.sin(playerOffset * i) * spawnRadius));
            Pos direction = BlockSumoGame.CENTER.sub(pos.x() + 0.5, BlockSumoGame.CENTER.y(), pos.z() + 0.5);

            spawns.add(BlockSumoGame.CENTER.add(pos).withDirection(direction));
        }

        return spawns;
    }

    private @NotNull Pos floorPos(Pos pos) {
        return new Pos(pos.blockX(), pos.blockY(), pos.blockZ());
    }

    public @NotNull Pos select() {
        LOGGER.info("Selecting spawn point (remaining before select: {})", this.spawns.size()); // Bug: spawns exceeded initial spawn point queue size
        Pos spawn = this.spawns.poll();
        if (spawn == null) {
            LOGGER.error("Spawns exceeded initial spawn point queue size");
            return BlockSumoGame.CENTER.add(0, 1, 0); // Happens during local testing
        }
        return spawn;
    }

}
