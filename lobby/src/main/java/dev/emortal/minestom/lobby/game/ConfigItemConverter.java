package dev.emortal.minestom.lobby.game;

import dev.emortal.minestom.lobby.config.ConfigItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.AttributeList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

final class ConfigItemConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigItemConverter.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    static @Nullable ItemStack convert(@NotNull ConfigItem item, @NotNull List<Component> lore) {
        if (item.material() == null) return null;
        Material material = Material.fromKey(item.material());
        if (material == null) {
            LOGGER.error("Failed to parse material '{}' from config item", item.material());
            return null;
        }

        Component displayName = MINI_MESSAGE.deserialize(item.name()).decoration(TextDecoration.ITALIC, false);
        return ItemStack.builder(material)
                .set(DataComponents.ITEM_NAME, displayName)
                .set(DataComponents.LORE, lore)
                .set(DataComponents.ATTRIBUTE_MODIFIERS, new AttributeList(List.of()))
                .build();
    }

    static @NotNull List<Component> convertLore(@NotNull ConfigItem item) {
        if (item.lore() == null) return new ArrayList<>();
        List<Component> lore = new ArrayList<>();
        for (String line : item.lore()) {
            lore.add(MINI_MESSAGE.deserialize(line).decoration(TextDecoration.ITALIC, false));
        }
        return lore;
    }

    static @NotNull List<Component> createDisplayItemLore(@NotNull ConfigItem item, boolean hasMaps, long playerCount) {
        List<Component> lore = convertLore(item);

        lore.add(Component.empty());
        lore.add(Component.text("Left click to queue", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        if (hasMaps) {
            lore.add(Component.text("Right click to select map", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        }

        if (playerCount > 0) {
            lore.add(Component.empty());
            lore.add(Component.text()
                    .append(Component.text("● ", NamedTextColor.GREEN))
                    .append(Component.text(playerCount, NamedTextColor.GREEN, TextDecoration.BOLD))
                    .append(Component.text(" playing", NamedTextColor.GREEN))
                    .build()
                    .decoration(TextDecoration.ITALIC, false));
        }

        return lore;
    }

    private ConfigItemConverter() {
    }
}
