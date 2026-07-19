package dev.emortal.minestom.lobby.game;

import dev.emortal.minestom.lobby.config.ConfigNPC;
import dev.emortal.minestom.lobby.config.GameModeConfig;
import dev.emortal.minestom.lobby.util.entity.MultilineHologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.click.ClickType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class GameNpcHandler {
    private static final Pos CIRCLE_CENTER = new Pos(0.5, 69.0, -29.0);
    private static final Pos LOOKING_POS = CIRCLE_CENTER.add(0, 0, 3);
    private static final double NPC_RADIUS = 5;

    private final @NotNull List<GameModeConfig> configs;
    private final @NotNull Instance instance;

    private final Map<String, Entity> npcs = new HashMap<>();
    private final Map<String, MultilineHologram> holograms = new HashMap<>();

    GameNpcHandler(@NotNull List<GameModeConfig> configs, @NotNull Instance instance) {
        this.configs = configs;
        this.instance = instance;

        // Do the initial render
        this.renderNpcs();
    }

    void renderNpcs() {
        this.removeAllNpcs();

        List<GameModeConfig> sortedConfigs = new ArrayList<>();
        for (GameModeConfig config : configs) {
            if (!config.enabled()) continue;
            if (config.displayNpc() == null) continue;
            sortedConfigs.add(config);
        }
        sortedConfigs.sort(Comparator.comparingInt(GameModeConfig::priority));

        long npcCount = sortedConfigs.size();

        int i = 0;
        for (GameModeConfig config : sortedConfigs) {
            ConfigNPC npc = config.displayNpc();

            Pos position = this.calculateNpcPosition(npcCount, i);
            this.createNpc(config, position);
            i++;
        }
    }

    private void removeAllNpcs() {
        this.npcs.forEach((fleetName, npc) -> npc.remove());
        this.holograms.forEach((fleetName, hologram) -> hologram.remove());

        this.npcs.clear();
        this.holograms.clear();
    }

    private @NotNull Pos calculateNpcPosition(long npcCount, int npcIndex) {
        if (npcCount == 1) return CIRCLE_CENTER;

        double angle = npcIndex * (Math.PI / (npcCount - 1));
        double x = Math.cos(angle) * NPC_RADIUS;
        double z = Math.sin(angle) * NPC_RADIUS / 1.5;
        return CIRCLE_CENTER.add(x, 0, -z).withLookAt(LOOKING_POS);
    }

    private void createNpc(@NotNull GameModeConfig config, @NotNull Pos pos) {
        ConfigNPC configNpc = config.displayNpc();
        if (configNpc == null) return;

        Entity npc = ConfigNpcConverter.convertNpc(configNpc, this.instance, pos, (player, clickType) -> {
            if (clickType == ClickType.LEFT_CLICK) {
                QueueGameClickHandler.leftClick(player, config);
            } else {
                QueueGameClickHandler.rightClick(player, config);
            }
        });
        this.npcs.put(config.fleetName(), npc);

        MultilineHologram hologram = ConfigNpcConverter.convertHologram(configNpc, this.instance, pos);
        this.holograms.put(config.fleetName(), hologram);
    }

    void updatePlayerCount(@NotNull String fleetName, long playerCount) {
        MultilineHologram hologram = this.holograms.get(fleetName);
        if (hologram == null) return;

        hologram.setLine(hologram.size() - 1, Component.text(playerCount + " playing", playerCount > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY));
    }
}
