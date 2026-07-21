package dev.emortal.minestom.minesweeper.game;

import dev.emortal.minestom.minesweeper.board.Board;
import dev.emortal.minestom.minesweeper.board.BoardWriter;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.UUID;
import java.util.stream.Collectors;

public class SaveHandler {

    private static final Path MAPS_PATH = Path.of("maps");
    private static final int SAVE_INTERVAL_TICKS = 20 * 60 * 15; // 15 minutes
    static {
        MAPS_PATH.toFile().mkdirs();
    }

    private boolean enableSave = true;
    private final Board board;
    private final Collection<UUID> uuids;
    private @Nullable Task autosaveTask = null;
    public SaveHandler(Board board, Collection<UUID> uuids) {
        this.board = board;
        this.uuids = uuids;
    }

    public void startAutosaveTask() {
        this.autosaveTask = board.getInstance().scheduler().buildTask(this::save)
                .delay(TaskSchedule.tick(SAVE_INTERVAL_TICKS))
                .repeat(TaskSchedule.tick(SAVE_INTERVAL_TICKS))
                .schedule();
    }

    public void save() {
        if (!enableSave) return;
        byte[] data = BoardWriter.write(board);
        try {
            Files.write(getPathFromUuids(uuids), data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete() {
        if (this.autosaveTask != null) this.autosaveTask.cancel();
        try {
            Files.delete(getPathFromUuids(uuids));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setEnableSave(boolean enableSave) {
        this.enableSave = enableSave;
    }

    public static Path getPathFromUuids(Collection<UUID> uuids) {
        String fileName = getFileNameFromUuids(uuids);
        return MAPS_PATH.resolve(fileName + ".mines");
    }

    private static String getFileNameFromUuids(Collection<UUID> uuids) {
        // sort uuids and remove duplicates
        String combined = uuids.stream()
                .map(UUID::toString)
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));

        // hash uuids
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] hashBytes = digest.digest(combined.getBytes(StandardCharsets.UTF_8));

        // convert bytes to hex
        return HexFormat.of().formatHex(hashBytes);
    }

}
