package dev.emortal.minestom.minesweeper.map;

import dev.emortal.minestom.minesweeper.board.Board;
import dev.emortal.minestom.minesweeper.util.NoTickEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.color.Color;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public record MapTheme(@NotNull Block unrevealed, @NotNull Block unrevealedAlt, @NotNull Block unrevealedAlt2, @NotNull Block unrevealedAlt3, @NotNull Block nothing, @NotNull Block mine,
                       @NotNull RevealedSquarePlacer revealedSquarePlacer) {
    public static final @NotNull MapTheme DEFAULT = new MapTheme(Block.LIME_CONCRETE, Block.LIME_CONCRETE_POWDER, Block.GREEN_CONCRETE, Block.GREEN_CONCRETE_POWDER, Block.SMOOTH_QUARTZ, Block.TNT,
            RevealedSquarePlacer.DISPLAY_ENTITY);

    private static final RGBLike[] COLORS = new RGBLike[]{
            new Color(0, 0, 0), // 0
            new Color(2, 2, 254), // 1
            new Color(64, 116, 64),
            new Color(254, 2, 2),
            new Color(1, 1, 132),
            new Color(132, 0, 0),
            new Color(3, 131, 133),
            new Color(132, 2, 132),
            new Color(117, 117, 117),
    };

    @FunctionalInterface
    public interface RevealedSquarePlacer {
        @NotNull RevealedSquarePlacer DISPLAY_ENTITY = (map, x, y, surroundingCount) -> {
            if (surroundingCount < 1) return;
            Instance instance = map.getInstance();

            Entity number = new NoTickEntity(EntityType.TEXT_DISPLAY);
            number.editEntityMeta(TextDisplayMeta.class, meta -> {
                Component text = Component.text(surroundingCount, TextColor.color(COLORS[surroundingCount]));
                meta.setText(text);
                meta.setBackgroundColor(0);
                meta.setHeight(0.3f);
                meta.setWidth(0.3f);
                meta.setScale(new Vec(3));
            });
            number.setInstance(instance, new Pos(x + 0.47, MinesweeperMapManager.FLOOR_HEIGHT + 1 + Vec.EPSILON, y + 0.92, 0F, -90F));
        };

        void revealSquare(@NotNull Board map, int x, int y, int surroundingCount);
    }

    public void showSolvedIndicator(Chunk chunk) {
        int x = chunk.getChunkX() * Chunk.CHUNK_SIZE_X;
        int z = chunk.getChunkZ() * Chunk.CHUNK_SIZE_Z;

        Entity number = new NoTickEntity(EntityType.BLOCK_DISPLAY);
        number.editEntityMeta(BlockDisplayMeta.class, meta -> {
            meta.setBlockState(Block.LIGHT_GRAY_STAINED_GLASS);
            meta.setHeight(32f);
            meta.setWidth(32f);
            meta.setViewRange(3f);
            meta.setScale(new Vec(Chunk.CHUNK_SIZE_X));
        });
        number.setInstance(chunk.getInstance(), new Pos(x, MinesweeperMapManager.FLOOR_HEIGHT - 14.92, z));
    }
}
