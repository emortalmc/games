package dev.emortal.minestom.minesweeper.util;

import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public enum TeamColor {

    // Ordinal is used by storage so changing order will affect stored data.
    RED(net.minestom.server.color.TeamColor.RED, Block.RED_CARPET),
    ORANGE(net.minestom.server.color.TeamColor.GOLD, Block.ORANGE_CARPET),
    CYAN(net.minestom.server.color.TeamColor.DARK_AQUA, Block.CYAN_CARPET),
    BLUE(net.minestom.server.color.TeamColor.BLUE, Block.BLUE_CARPET),
    PINK(net.minestom.server.color.TeamColor.LIGHT_PURPLE, Block.PINK_CARPET),
    PURPLE(net.minestom.server.color.TeamColor.DARK_PURPLE, Block.PURPLE_CARPET),
    AQUA(net.minestom.server.color.TeamColor.AQUA, Block.CYAN_CARPET),
    YELLOW(net.minestom.server.color.TeamColor.YELLOW, Block.YELLOW_CARPET),
    DARK_AQUA(net.minestom.server.color.TeamColor.DARK_AQUA, Block.CYAN_CARPET),
    LIGHT_GRAY(net.minestom.server.color.TeamColor.GRAY, Block.LIGHT_GRAY_CARPET);

    private static final TeamColor[] VALUES = values();

    public static @NotNull TeamColor fromId(byte id) {
        return VALUES[id];
    }

    private final @NotNull net.minestom.server.color.TeamColor color;
    private final @NotNull Block carpet;

    TeamColor(@NotNull net.minestom.server.color.TeamColor color, @NotNull Block carpet) {
        this.color = color;
        this.carpet = carpet;
    }

    public @NotNull net.minestom.server.color.TeamColor teamColor() {
        return color;
    }

    public @NotNull NamedTextColor color() {
        return NamedTextColor.nearestTo(this.color.textColor());
    }

    public @NotNull Block carpet() {
        return this.carpet;
    }
}
