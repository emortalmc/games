package dev.emortal.minestom.minesweeper.board;

import dev.emortal.minestom.minesweeper.map.MapManager;
import dev.emortal.minestom.minesweeper.map.MapTheme;
import dev.emortal.minestom.minesweeper.util.Direction8;
import dev.emortal.minestom.minesweeper.util.Flag;
import dev.emortal.minestom.minesweeper.util.TeamColor;
import dev.emortal.minestom.minesweeper.util.Vec2;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.CoordConversion;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class Board {

    private static final Tag<Boolean> POPULATED_TAG = Tag.Boolean("minesPopulated");
    private static final Tag<Boolean> MINE_TAG = Tag.Boolean("blockHasMine");
    public static final Tag<Set<Vec2>> CLICKS_TAG = Tag.Transient("chunkClicks");
    public static final Tag<Set<Flag>> FLAGS_TAG = Tag.Transient("chunkFlags");

    private final int width;
    private final int height;
    private final boolean infinite;
    private final long seed;
    private final long startingTicks;
    private int lives;
    private final @NotNull Instance instance;
    private final @NotNull MapTheme theme;

    private final @NotNull Set<Vec2> solvedChunks = new HashSet<>();
    private final @NotNull Set<Vec2> touchedChunks = new HashSet<>();

    public Board(long seed, long startingTicks, int lives, @NotNull Instance instance, @NotNull MapTheme theme) {
        this.width = 0;
        this.height = 0;
        this.infinite = true;
        this.seed = seed;
        this.startingTicks = startingTicks;
        this.lives = lives;
        this.instance = instance;
        this.theme = theme;
    }

    public Board(int width, int height, long seed, long startingTicks, int lives, @NotNull Instance instance, @NotNull MapTheme theme) {
        this.infinite = false;
        this.width = width;
        this.height = height;
        this.seed = seed;
        this.startingTicks = startingTicks;
        this.lives = lives;
        this.instance = instance;
        this.theme = theme;
    }

    public boolean isMine(int x, int y) {
        Chunk chunk = this.instance.getChunkAt(x, y);
        if (chunk == null) {
            chunk = this.instance.loadChunk(new Vec(x, MapManager.FLOOR_HEIGHT, y)).join();
        }

        if (!chunk.hasTag(POPULATED_TAG)) {
            populateWithMines(chunk);
        }

        return isMine(this.instance.getBlock(x, MapManager.FLOOR_HEIGHT, y));
    }

    public boolean isMine(Block block) {
        return block.hasTag(MINE_TAG);
    }

    public boolean isRevealed(int x, int y) {
        Block block = this.instance.getBlock(x, MapManager.FLOOR_HEIGHT, y, Block.Getter.Condition.TYPE);
        if (block == null) return false;

        return isRevealed(block);
    }

    public boolean isRevealed(Block block) {
        return block.compare(this.theme.nothing());
    }

    public Block getFlag(int x, int y) {
        return this.instance.getBlock(x, MapManager.FLOOR_HEIGHT + 1, y);
    }

    public boolean isFlagged(int x, int y) {
        Block block = this.instance.getBlock(x, MapManager.FLOOR_HEIGHT + 1, y, Block.Getter.Condition.TYPE);
        if (block == null) return false;
        return isFlagged(block);
    }

    public boolean isFlagged(@NotNull Block block) {
        return MinecraftServer.process().blocks().getTag(Key.key("minecraft:wool_carpets")).contains(block);
    }

    public int getUnrevealedCount() {
        int unrevealed = 0;
        for (int x = 0; x < height; x++) {
            for (int y = 0; y < width; y++) {
                if (!isRevealed(x, y)) unrevealed++;
            }
        }

        return unrevealed;
    }

    public boolean isSolved(Chunk chunk) {
        if (solvedChunks.contains(new Vec2(chunk.getChunkX(), chunk.getChunkZ()))) return true;

        for (int x = 0; x < Chunk.CHUNK_SIZE_X; x++) {
            for (int y = 0; y < Chunk.CHUNK_SIZE_Z; y++) {
                Block block = chunk.getBlock(x, MapManager.FLOOR_HEIGHT, y);

                if (!isRevealed(block) && !isMine(block)) {
                    return false;
                }
            }
        }

        return true;
    }

    public void revealSolved(Chunk chunk, TeamColor color) {
        for (int relX = 0; relX < Chunk.CHUNK_SIZE_X; relX++) {
            int x = chunk.getChunkX() * Chunk.CHUNK_SIZE_X + relX;
            for (int relY = 0; relY < Chunk.CHUNK_SIZE_Z; relY++) {
                int y = chunk.getChunkZ() * Chunk.CHUNK_SIZE_Z + relY;
                if (isMine(x, y)) {
                    this.addFlagIfMissing(new Flag(new Vec2(x, y), color), chunk);
                    continue;
                }
                revealAround(x, y);
            }
        }

        this.theme.showSolvedIndicator(chunk);
    }

    private void addMine(int x, int y) {
        Block prevBlock = this.instance.getBlock(x, MapManager.FLOOR_HEIGHT, y);
        this.instance.setBlock(x, MapManager.FLOOR_HEIGHT, y, prevBlock.withTag(MINE_TAG, true));
    }

    public byte getMinesAround(int x, int y) {
        byte mines = 0;

        for (Direction8 direction : Direction8.VALUES) {
            int newX = x + direction.offsetX();
            int newY = y + direction.offsetY();
            if (this.isOutOfBounds(newX, newY)) continue;

            if (this.isMine(newX, newY)) {
                mines++;
            }
        }

        return mines;
    }

    public void reveal(int x, int y, Block.Setter blockSetter) {
        boolean mine = isMine(x, y);
        if (mine) return;

        blockSetter.setBlock(x, MapManager.FLOOR_HEIGHT, y, this.theme.nothing());

        int mines = getMinesAround(x, y);
        if (mines > 0) {
            this.theme.revealedSquarePlacer().revealSquare(this, x, y, mines);
        }
    }

    public Set<Chunk> revealAround(int x, int y) {
        Set<Chunk> affectedChunks = new HashSet<>();
        revealAround(x, y, affectedChunks);
        return affectedChunks;
    }

    private void revealAround(int x, int y, Set<Chunk> affectedChunks) {
        Chunk chunk = instance.getChunk(CoordConversion.globalToChunk(x), CoordConversion.globalToChunk(y));

        reveal(x, y, chunk);

        affectedChunks.add(chunk);

        byte minesAround1 = getMinesAround(x, y);
        if (minesAround1 > 0) return;

        for (Direction8 direction : Direction8.VALUES) {
            int newX = x + direction.offsetX();
            int newY = y + direction.offsetY();

            if (this.isOutOfBounds(newX, newY)) continue;
            if (this.isRevealed(newX, newY)) continue;

            this.revealAround(newX, newY, affectedChunks);
        }
    }

    public boolean isOutOfBounds(int x, int y) {
        if (infinite) return false;
        return x < 0 || x >= height || y < 0 || y >= width;
    }

    public void populateWithMines(Chunk chunk) {
        if (chunk.hasTag(POPULATED_TAG)) return; // already populated

        double base = 0.09;
        long dx = Math.abs(chunk.getChunkX());
        long dz = Math.abs(chunk.getChunkZ());
        long chebyshev = Math.max(dx, dz);

        double difficulty = base + (chebyshev * 0.01);
        if (difficulty > 0.19) difficulty = 0.19;

        populateWithMines(chunk, difficulty);
    }

    public void populateWithMines(Chunk chunk, double minePercent) {
        chunk.setTag(POPULATED_TAG, true);

        int mines = (int) (minePercent * Chunk.CHUNK_SIZE_X * Chunk.CHUNK_SIZE_Z);

        Random random = createRandom(seed, chunk.getChunkX(), chunk.getChunkZ());

        int i = 0;
        while (mines > 0 && i < 256) {
            int randX = random.nextInt(Chunk.CHUNK_SIZE_X);
            int randY = random.nextInt(Chunk.CHUNK_SIZE_Z);
            int x = chunk.getChunkX() * Chunk.CHUNK_SIZE_X + randX;
            int y = chunk.getChunkZ() * Chunk.CHUNK_SIZE_Z + randY;

            i++;

            if (isMine(x, y)) continue;

            addMine(x, y);

            mines--;
        }
    }

    public void revealMines() {
        instance.scheduleNextTick(a -> {
            AbsoluteBlockBatch batch = new AbsoluteBlockBatch();

            for (Chunk chunk : instance.getChunks()) {
                for (int relX = 0; relX < Chunk.CHUNK_SIZE_X; relX++) {
                    int x = chunk.getChunkX() * Chunk.CHUNK_SIZE_X + relX;
                    for (int relY = 0; relY < Chunk.CHUNK_SIZE_Z; relY++) {
                        int y = chunk.getChunkZ() * Chunk.CHUNK_SIZE_Z + relY;

                        if (isOutOfBounds(x, y)) continue;
                        if (!isMine(x, y)) continue;
                        batch.setBlock(x, MapManager.FLOOR_HEIGHT, y, this.theme.mine());
                    }
                }
            }

            batch.apply(this.instance, null);
        });
    }

    public @NotNull Instance getInstance() {
        return instance;
    }

    public @NotNull MapTheme getTheme() {
        return theme;
    }

    public long getSeed() {
        return seed;
    }

    public long getTicks() {
        return this.startingTicks + instance.getWorldAge();
    }

    public long getStartingTicks() {
        return startingTicks;
    }

    public int getLives() {
        return lives;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    public boolean isInfinite() {
        return infinite;
    }

    public void addClick(Vec2 pos, Chunk chunk) {
        Set<Vec2> clicks = chunk.getTag(CLICKS_TAG);
        if (clicks == null) {
            clicks = new HashSet<>();
            chunk.setTag(CLICKS_TAG, clicks);
            addTouchedChunk(chunk);
        }

        clicks.add(pos);
    }

    public void addFlagIfMissing(Flag flag, Chunk chunk) {
        Set<Flag> flags = chunk.getTag(FLAGS_TAG);
        if (flags == null) {
            flags = new HashSet<>();
            chunk.setTag(FLAGS_TAG, flags);
            addTouchedChunk(chunk);
        }

        if (flags.stream().anyMatch(f -> f.pos().equals(flag.pos()))) {
            return;
        }
        chunk.setBlock(flag.pos().x(), MapManager.FLOOR_HEIGHT + 1, flag.pos().y(), flag.color().carpet());
        chunk.sendChunk();

        flags.add(flag);
    }

    public void addFlag(Flag flag, Block block, Chunk chunk) {
        Set<Flag> flags = chunk.getTag(FLAGS_TAG);
        if (flags == null) {
            flags = new HashSet<>();
            chunk.setTag(FLAGS_TAG, flags);
            addTouchedChunk(chunk);
        }

        if (flags.stream().anyMatch(f -> f.pos().equals(flag.pos()))) {
            return;
        }
        chunk.setBlock(flag.pos().x(), MapManager.FLOOR_HEIGHT + 1, flag.pos().y(), block);
        chunk.sendChunk();

        flags.add(flag);
    }

    public void removeFlag(Flag flag, Chunk chunk) {
        chunk.setBlock(flag.pos().x(), MapManager.FLOOR_HEIGHT + 1, flag.pos().y(), Block.AIR);
        chunk.sendChunk();

        Set<Flag> flags = chunk.getTag(FLAGS_TAG);
        if (flags == null) return;
        flags.remove(flag);
    }

    public void addSolvedChunk(Chunk chunk) {
        solvedChunks.add(new Vec2(chunk.getChunkX(), chunk.getChunkZ()));
    }

    public @NotNull Set<Chunk> getTouchedChunks() {
        Set<Chunk> chunks = new HashSet<>();
        for (Vec2 touchedChunk : touchedChunks) {
            chunks.add(instance.getChunk(touchedChunk.x(), touchedChunk.y()));
        }

        return chunks;
    }

    public void addTouchedChunk(Chunk chunk) {
        touchedChunks.add(new Vec2(chunk.getChunkX(), chunk.getChunkZ()));
    }

    public @NotNull Set<Vec2> getSolvedChunks() {
        return solvedChunks;
    }

    public Random createRandom(final long seed, final int x, final int z) {
        final Random random = new Random(seed);

        long long1 = random.nextLong();
        long long2 = random.nextLong();
        long newSeed = (long) x * long1 ^ (long) z * long2 ^ seed;
        random.setSeed(newSeed);

        return random;
    }
}
