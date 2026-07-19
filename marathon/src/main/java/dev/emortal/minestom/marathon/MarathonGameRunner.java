package dev.emortal.minestom.marathon;

import dev.emortal.messaging.types.MarathonData;
import dev.emortal.minestom.core.game.Game;
import dev.emortal.minestom.core.game.config.GameCreationInfo;
import dev.emortal.minestom.marathon.listener.MovementListener;
import net.minestom.server.entity.Player;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static dev.emortal.minestom.marathon.MarathonGame.RESET_POINT;

public final class MarathonGameRunner extends Game {
    private final Map<UUID, MarathonGame> games = Collections.synchronizedMap(new HashMap<>());
    private final Map<UUID, MarathonData> playerData;
    private final RegistryKey<DimensionType> dimension;

    public MarathonGameRunner(@NotNull GameCreationInfo creationInfo,
                              @NotNull RegistryKey<DimensionType> dimension, @NotNull Map<UUID, MarathonData> playerData) {
        super(creationInfo, null);
        this.playerData = playerData;
        this.dimension = dimension;

        MovementListener movementListener = new MovementListener(this);

        this.getEventNode()
                .addListener(PlayerMoveEvent.class, movementListener::onMove)
                .addListener(PlayerSwapItemEvent.class, event -> event.setCancelled(true))
                .addListener(ItemDropEvent.class, event -> event.setCancelled(true));
    }

    @Override
    public void start() {
    }

    @Override
    public void onPreJoin(@NotNull Player player) {
        player.setRespawnPoint(RESET_POINT.add(0, 1, 0));

        MarathonData data = this.playerData.getOrDefault(player.getUuid(), Main.DEFAULT_PLAYER_DATA);

        MarathonGame game = this.games.computeIfAbsent(player.getUuid(), uuid -> new MarathonGame(
                this.dimension, player, data));

        player.eventNode().addListener(PlayerSpawnEvent.class, event -> game.refreshInventory());
    }

    @Override
    public void onJoin(@NotNull Player player) {
        MarathonData data = this.playerData.getOrDefault(player.getUuid(), Main.DEFAULT_PLAYER_DATA);

        MarathonGame game = this.games.computeIfAbsent(player.getUuid(), uuid -> new MarathonGame(
                this.dimension, player, data));

        game.onJoin(player);
    }

    @Override
    public void onLeave(@NotNull Player player) {
        MarathonGame game = this.games.get(player.getUuid());
        if (game == null) return;
        game.onLeave(player);
    }

    @Override
    public void cleanUp() {
        for (MarathonGame game : this.games.values()) {
            game.cleanUp();
        }
    }

    @Override
    public @NotNull Instance getSpawningInstance(@NotNull Player player) {
        MarathonGame game = this.games.get(player.getUuid());
        if (game == null) {
            throw new IllegalStateException("No game found for player " + player.getUsername() + " when instance requested!");
        }
        return game.getInstance();
    }

    public @Nullable MarathonGame getGameForPlayer(@NotNull Player player) {
        return this.games.get(player.getUuid());
    }
}
