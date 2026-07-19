package dev.emortal.minestom.lobby.game;

import dev.emortal.messaging.message.Channel;
import dev.emortal.messaging.message.MapVoteMessage;
import dev.emortal.minestom.core.EmortalServer;
import dev.emortal.minestom.lobby.config.ConfigItem;
import dev.emortal.minestom.lobby.config.ConfigMap;
import dev.emortal.minestom.lobby.config.GameModeConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public final class MapSelectorInventory extends Inventory {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapSelectorInventory.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String TITLE_FORMAT = "<mode><reset> map selector";
    private static final String ERR_MAP_NOT_FOUND = "<red>Map <map> not found for <mode></red>";
    private static final Component ERR_UNKNOWN = Component.text("An unknown error occurred", NamedTextColor.RED);

    private final GameModeConfig mode;
    private final boolean isQueued;

    private final Map<Integer, ConfigMap> slotMaps = new HashMap<>();

    /**
     * @param mode       the mode to select a map for
     * @param isQueued   if queued, instead of queueing for a map it will modify the current queue's selection.
     */
    public MapSelectorInventory(@NotNull GameModeConfig mode, boolean isQueued) {
        super(InventoryType.CHEST_3_ROW, MINI_MESSAGE.deserialize(TITLE_FORMAT, Placeholder.unparsed("mode", mode.displayItem().name())));

        this.isQueued = isQueued;
        this.mode = mode;

        this.loadMaps(mode);
        super.eventNode().addListener(InventoryPreClickEvent.class, this::handleClick);
    }

    private void loadMaps(@NotNull GameModeConfig config) {
        Map<String, ConfigMap> maps = config.maps();
        if (maps == null) return;

        for (ConfigMap map : maps.values()) {
            ConfigItem item = map.displayItem();
            this.slotMaps.put(item.slot(), map);

            ItemStack stack = ConfigItemConverter.convert(item, ConfigItemConverter.convertLore(item));
            if (stack == null) continue;

            super.setItemStack(item.slot(), stack);
        }
    }

    private void handleClick(@NotNull InventoryPreClickEvent event) {
        Player player = event.getPlayer();
        int slot = event.getSlot();

        event.setCancelled(true);

        ConfigMap map = this.slotMaps.get(slot);
        if (map == null) return; // nothing in slot

        EmortalServer.getRedis().sendMessage(Channel.PROXY, new MapVoteMessage(player.getUuid(), map.id()));

        player.closeInventory();
    }
}
