package dev.emortal.minestom.lobby.game;

import dev.emortal.api.liveconfigparser.configs.ConfigProvider;
import dev.emortal.api.liveconfigparser.configs.common.ConfigItem;
import dev.emortal.api.liveconfigparser.configs.gamemode.GameModeConfig;
import dev.emortal.api.service.matchmaker.MatchmakerService;
import dev.emortal.api.service.playertracker.PlayerTrackerService;
import dev.emortal.minestom.core.utils.Menu;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class ServerSelectorMenu extends Menu {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerSelectorMenu.class);

    private final @Nullable MatchmakerService matchmaker;
    private final @Nullable PlayerTrackerService playerTracker;
    private final @NotNull ConfigProvider<GameModeConfig> configProvider;
    private final Map<String, Long> playerCounts;

    public ServerSelectorMenu(Player player, @Nullable MatchmakerService matchmaker, @Nullable PlayerTrackerService playerTracker, @NotNull ConfigProvider<GameModeConfig> configProvider) {
        super(player, InventoryType.CHEST_4_ROW, "Pick a game, any game!");
        this.matchmaker = matchmaker;
        this.playerTracker = playerTracker;
        this.configProvider = configProvider;
        this.playerCounts = new HashMap<>();

        MinecraftServer.getSchedulerManager().buildTask(() -> {
            this.fetchPlayerCounts();
            this.refreshInventory();
        })
                .repeat(2, TimeUnit.SECOND)
                .schedule();
    }

    private void fetchPlayerCounts() {
        if (this.playerTracker == null) {
            return;
        }

        var fleetNames = this.configProvider.allConfigs().stream().map(GameModeConfig::fleetName).toList();

        try {
            this.playerCounts.clear();
            this.playerCounts.putAll(this.playerTracker.getFleetPlayerCounts(fleetNames));
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to get player counts for fleets", exception);
        }
    }

    private void refreshInventory() {
        for (GameModeConfig config : this.configProvider.allConfigs()) {
            if (!config.enabled()) {
                continue;
            }

            int slot = Objects.requireNonNull(config.displayItem()).slot();
            long playerCount = this.playerCounts.getOrDefault(config.fleetName(), 0L);

            this.set(slot, createItemStack(config, playerCount), click -> {
                if (click instanceof Click.Left) {
                    QueueGameClickHandler.leftClick(this.player, config, this.matchmaker);
                    this.player.closeInventory();
                } else if (click instanceof Click.Right) {
                    QueueGameClickHandler.rightClick(this.player, config, this.matchmaker);
                }
            });
        }
    }

    private static ItemStack createItemStack(GameModeConfig config, long playerCount) {
        ConfigItem item = Objects.requireNonNull(config.displayItem());
        boolean hasMaps = config.maps() != null && !config.maps().isEmpty();
        List<Component> lore = ConfigItemConverter.createDisplayItemLore(item, hasMaps, playerCount);
        return ConfigItemConverter.convert(item, lore);
    }
}
