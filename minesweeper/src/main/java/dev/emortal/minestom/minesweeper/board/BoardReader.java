package dev.emortal.minestom.minesweeper.board;

import com.github.luben.zstd.Zstd;
import dev.emortal.minestom.minesweeper.map.MapTheme;
import dev.emortal.minestom.minesweeper.map.MinesweeperMapManager;
import dev.emortal.minestom.minesweeper.util.Flag;
import dev.emortal.minestom.minesweeper.util.TeamColor;
import dev.emortal.minestom.minesweeper.util.Vec2;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.minestom.server.network.NetworkBuffer.*;

public class BoardReader {

    public static Board read(byte[] data, Instance instance) {
        var buffer = NetworkBuffer.wrap(data, 0, data.length);
        buffer.writeIndex(data.length); // Set write index to end so readableBytes returns remaining bytes

        var magicNumber = buffer.read(INT);
        assertThat(magicNumber == BoardWriter.MAGIC_NUMBER, "Invalid magic number");

        short version = buffer.read(SHORT);
        validateVersion(version);

        long seed = buffer.read(LONG);
        long ticks = buffer.read(LONG);
        int lives = buffer.read(VAR_INT);

        var compressedDataLength = buffer.read(VAR_INT);

        // Replace the buffer with the decompressed version
        var bytes = Zstd.decompress(buffer.read(RAW_BYTES), compressedDataLength);
        var newBuffer = NetworkBuffer.wrap(bytes, 0, 0);
        newBuffer.writeIndex(bytes.length);
        buffer = newBuffer;

        Board board = new Board(seed, ticks, lives, instance, MapTheme.DEFAULT);

        List<Chunk> chunks = new ArrayList<>();

        int chunkCount = buffer.read(VAR_INT);
        for (int i = 0; i < chunkCount; i++) {
            Integer chunkX = buffer.read(VAR_INT);
            Integer chunkZ = buffer.read(VAR_INT);

            Chunk chunk = board.getInstance().loadChunk(chunkX, chunkZ).join();
            board.populateWithMines(chunk);
            board.addTouchedChunk(chunk);
            readChunk(newBuffer, board, chunk);

            chunks.add(chunk);
        }

        for (Chunk chunk : chunks) {
            Set<Vec2> clicks = chunk.getTag(Board.CLICKS_TAG);
            if (clicks != null) {
                for (Vec2 click : clicks) {
                    board.revealAround(click.x(), click.y());
                }
            }
        }

        return board;
    }

    private static void readChunk(NetworkBuffer buffer, Board board, Chunk chunk) {
        if (buffer.read(BOOLEAN)) { // if chunk solved
            board.addSolvedChunk(chunk);
            board.revealSolved(chunk, TeamColor.RED); // Might want to save colours on saved chunks one day
            return;
        }

        int clickCount = buffer.read(VAR_INT);
        Set<Vec2> clicks = new HashSet<>(clickCount);
        for (int i = 0; i < clickCount; i++) {
            Integer clickX = buffer.read(INT);
            Integer clickY = buffer.read(INT);
            clicks.add(new Vec2(clickX, clickY));
        }
        chunk.setTag(Board.CLICKS_TAG, clicks);

        int flagsCount = buffer.read(VAR_INT);
        Set<Flag> flags = new HashSet<>(flagsCount);
        for (int i = 0; i < flagsCount; i++) {
            Integer flagX = buffer.read(INT);
            Integer flagY = buffer.read(INT);
            TeamColor color = TeamColor.values()[buffer.read(INT)];
            flags.add(new Flag(new Vec2(flagX, flagY), color));
            chunk.setBlock(flagX, MinesweeperMapManager.FLOOR_HEIGHT + 1, flagY, Block.RED_CARPET);
        }
        chunk.setTag(Board.FLAGS_TAG, flags);
    }

    static void validateVersion(int version) {
        var invalidVersionError = String.format("Unsupported save version. Up to %d is supported, found %d.",
                BoardWriter.LATEST_VERSION, version);
        assertThat(version <= BoardWriter.LATEST_VERSION, invalidVersionError);
    }

    static void assertThat(boolean condition, @NotNull String message) {
        if (!condition) throw new Error(message);
    }

    public static class Error extends RuntimeException {
        private Error(String message) {
            super(message);
        }
    }

}
