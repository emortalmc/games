package dev.emortal.minestom.blocksumo.team;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TeamColor {

    private static final ItemStack[] ITEM_STACKS = new ItemStack[] {
            ItemStack.of(Material.BLACK_WOOL, 64),
            ItemStack.of(Material.BLUE_WOOL, 64),
            ItemStack.of(Material.GREEN_WOOL, 64),
            ItemStack.of(Material.RED_WOOL, 64),
            ItemStack.of(Material.PURPLE_WOOL, 64),
            ItemStack.of(Material.ORANGE_WOOL, 64),
            ItemStack.of(Material.LIGHT_GRAY_WOOL, 64),
            ItemStack.of(Material.GRAY_WOOL, 64),
            ItemStack.of(Material.LIGHT_BLUE_WOOL, 64),
            ItemStack.of(Material.LIME_WOOL, 64),
            ItemStack.of(Material.CYAN_WOOL, 64),
            ItemStack.of(Material.CYAN_WOOL, 64),
            ItemStack.of(Material.RED_WOOL, 64),
            ItemStack.of(Material.MAGENTA_WOOL, 64),
            ItemStack.of(Material.YELLOW_WOOL, 64)
    };

    private final RGBLike exactColor;
    private final net.minestom.server.color.TeamColor teamColor;
    private final ItemStack item;

    public TeamColor(RGBLike exactColor) {
        this.exactColor = exactColor;
        NamedTextColor nearest = NamedTextColor.nearestTo(TextColor.color(exactColor));
        this.teamColor = getTeamColor(nearest);
        this.item = ITEM_STACKS[colourIndex(nearest)];
    }

    @NotNull
    public ItemStack getWoolItem() {
        return this.item;
    }

    public RGBLike getExactColor() {
        return exactColor;
    }
    public TextColor getTextColor() {
        return TextColor.color(exactColor);
    }

    public NamedTextColor getNamedTextColor() {
        return NamedTextColor.nearestTo(teamColor.textColor());
    }

    public net.minestom.server.color.TeamColor getTeamColor() {
        return teamColor;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TeamColor otherCol)) return false;
        return getTextColor().compareTo(otherCol.getTextColor()) == 0;
    }

    private @Nullable net.minestom.server.color.TeamColor getTeamColor(NamedTextColor color) {
        net.minestom.server.color.TeamColor[] colours = net.minestom.server.color.TeamColor.values();
        for (net.minestom.server.color.TeamColor colour : colours) {
            if (colour.textColor() == color) {
                return colour;
            }
        }
        return null;
    }

    private int colourIndex(NamedTextColor color) {
        net.minestom.server.color.TeamColor[] colours = net.minestom.server.color.TeamColor.values();

        for (int i = 0; i < colours.length; i++) {
            if (colours[i].textColor().equals(color)) {
                return i;
            }
        }
        return -1;
    }

}
