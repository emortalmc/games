package dev.emortal.minestom.lobby.game;

import dev.emortal.messaging.message.OnlinePlayersMessage;
import dev.emortal.minestom.core.EmortalServer;
import dev.emortal.minestom.lobby.LobbyEvents;
import dev.emortal.minestom.lobby.config.ConfigItem;
import dev.emortal.minestom.lobby.config.GameModeConfig;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ServerSelector {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerSelector.class);

    private final @NotNull List<GameModeConfig> configs;
    private final @NotNull GameNpcHandler npcHandler;

    private final Inventory inventory = new Inventory(InventoryType.CHEST_4_ROW, "Pick a game, any game!");
    private final Map<Integer, GameModeConfig> slotToGameMode = new HashMap<>();

    public ServerSelector(@NotNull Instance instance,
                          @NotNull EventNode<Event> eventNode, @NotNull List<GameModeConfig> configs) {
        this.configs = configs;
        this.npcHandler = new GameNpcHandler(configs, instance);

        this.registerListeners(eventNode);

        for (GameModeConfig config : configs) {
            if (!config.enabled()) continue;

            this.createDisplayItem(config, 0);
        }

        MinecraftServer.getGlobalEventHandler().addListener(InventoryPreClickEvent.class, e -> {
            if (e.getInventory() == this.inventory) return;
            if (e.getPlayer().getOpenInventory() != this.inventory) return;
            e.setCancelled(true);
        });

        this.inventory.eventNode().addListener(InventoryPreClickEvent.class, this::handleInventoryClick);

        EmortalServer.getRedis().addMessageHandler(OnlinePlayersMessage.class, (_, msg) -> {
            this.updatePlayerCounts(msg.online());
        });
    }

    private void handleInventoryClick(@NotNull InventoryPreClickEvent event) {
        Player player = event.getPlayer();
        int slot = event.getSlot();

        event.setCancelled(true);

        GameModeConfig config = this.slotToGameMode.get(slot);
        if (config == null) return; // clicked empty slot

        if (event.getClick() instanceof Click.Left) {
            QueueGameClickHandler.leftClick(player, config);
            player.closeInventory();
        }
        if (event.getClick() instanceof Click.Right) {
            QueueGameClickHandler.rightClick(player, config);
        }
    }

    private void registerListeners(@NotNull EventNode<Event> eventNode) {
        eventNode.addListener(PlayerUseItemEvent.class, event -> {
            if (event.getHand() != PlayerHand.MAIN) return;

            Player player = event.getPlayer();
            if (player.getItemInMainHand().hasTag(LobbyEvents.SERVER_SELECTOR_TAG)) player.openInventory(this.inventory);
        });
    }

    private void createDisplayItem(@NotNull GameModeConfig config, int playerCount) {
        ConfigItem item = config.displayItem();
        if (item == null) return; // If the item is null we have no item to create
        if (!config.enabled()) return; // If the config isn't enabled then we don't want to create an item

        List<Component> lore = ConfigItemConverter.createDisplayItemLore(item, config.maps() != null && !config.maps().isEmpty(), playerCount);
        ItemStack stack = ConfigItemConverter.convert(item, lore);
        if (stack == null) return;

        this.addDisplayItem(config, item, stack);
    }

    private void addDisplayItem(@NotNull GameModeConfig config, @NotNull ConfigItem item, @NotNull ItemStack stack) {
        this.slotToGameMode.put(item.slot(), config);
        this.inventory.setItemStack(item.slot(), stack);
    }

    private void removeDisplayItem(@NotNull ConfigItem item) {
        this.slotToGameMode.remove(item.slot());
        this.inventory.setItemStack(item.slot(), ItemStack.AIR);
    }

    private void updatePlayerCounts(@NotNull Map<String, Integer> playerCounts) {
        for (Map.Entry<String, Integer> entry : playerCounts.entrySet()) {
            this.npcHandler.updatePlayerCount(entry.getKey(), entry.getValue());

            for (GameModeConfig config : configs) {
                if (!config.enabled()) continue;
                if (!config.fleetName().equals(entry.getKey())) continue;
                if (config.displayItem() == null) continue;

                this.updatePlayerCountInDisplayItem(config, entry.getValue());
            }
        }
    }

    private void updatePlayerCountInDisplayItem(@NotNull GameModeConfig config, long playerCount) {
        ConfigItem item = config.displayItem();
        if (item == null) return;

        boolean hasMaps = config.maps() != null && !config.maps().isEmpty();
        int slot = item.slot();

        ItemStack stack = this.inventory.getItemStack(slot);
        if (stack.isAir()) return;

        List<Component> newLore = ConfigItemConverter.createDisplayItemLore(item, hasMaps, playerCount);
        ItemStack newStack = stack.withLore(newLore);

        this.addDisplayItem(config, item, newStack);
    }
}
