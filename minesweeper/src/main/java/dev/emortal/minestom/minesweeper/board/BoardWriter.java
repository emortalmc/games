package dev.emortal.minestom.minesweeper.board;

import com.github.luben.zstd.Zstd;
import dev.emortal.minestom.minesweeper.util.Flag;
import dev.emortal.minestom.minesweeper.util.Vec2;
import net.minestom.server.instance.Chunk;
import net.minestom.server.network.NetworkBuffer;

import java.util.HashSet;
import java.util.Set;

import static net.minestom.server.network.NetworkBuffer.*;

public class BoardWriter {

    public static final int MAGIC_NUMBER = 0x4D696E65; // `Mine`
    public static final short LATEST_VERSION = 1;

    public static byte[] write(Board board) {
        var contentBytes = NetworkBuffer.makeArray(content -> {
            Set<Chunk> chunks = board.getTouchedChunks();
            content.write(VAR_INT, chunks.size());
            for (Chunk chunk : chunks) {
                writeChunk(content, board, chunk);
            }
        });

        // Create final buffer
        return NetworkBuffer.makeArray(buffer -> {
            buffer.write(INT, MAGIC_NUMBER);
            buffer.write(SHORT, LATEST_VERSION);
            buffer.write(LONG, board.getSeed());
            buffer.write(LONG, board.getTicks());
            buffer.write(VAR_INT, board.getLives());
            buffer.write(VAR_INT, contentBytes.length);
            buffer.write(RAW_BYTES, Zstd.compress(contentBytes));
        });
    }

    private static void writeChunk(NetworkBuffer content, Board board, Chunk chunk) {
        content.write(VAR_INT, chunk.getChunkX());
        content.write(VAR_INT, chunk.getChunkZ());

        boolean solved = board.getSolvedChunks().contains(new Vec2(chunk.getChunkX(), chunk.getChunkZ()));
        content.write(BOOLEAN, solved);

        if (solved) return;

        Set<Vec2> clicks = chunk.getTag(Board.CLICKS_TAG);
        if (clicks == null) clicks = new HashSet<>();
        content.write(VAR_INT, clicks.size());
        for (Vec2 click : clicks) {
            content.write(INT, click.x());
            content.write(INT, click.y());
        }
        Set<Flag> flags = chunk.getTag(Board.FLAGS_TAG);
        if (flags == null) flags = new HashSet<>();
        content.write(VAR_INT, flags.size());
        for (Flag flag : flags) {
            content.write(INT, flag.pos().x());
            content.write(INT, flag.pos().y());
            content.write(INT, flag.color().ordinal());
        }
    }

}
