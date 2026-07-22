package dev.emortal.minestom.core.game;

import dev.emortal.messaging.types.GameInfo;
import dev.emortal.minestom.core.game.config.GameCreationInfo;
import dev.emortal.minestom.core.game.util.GameEventPredicates;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.timer.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class PreGameInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreGameInitializer.class);

    private final @NotNull GameManager gameManager;

    private final @NotNull GameInfo info;
    private final @NotNull Game game;

    private final @NotNull EventNode<Event> preGameNode;
    private @Nullable Task startTimeOutTask = null; // called if not all players have joined and determines whether to start the game or cancel it.

    private final AtomicInteger playerCount = new AtomicInteger();

    public PreGameInitializer(@NotNull GameManager gameManager, @NotNull GameInfo info, @NotNull Game game) {
        this.gameManager = gameManager;
        this.info = info;
        this.game = game;

        GameCreationInfo creationInfo = game.getCreationInfo();

        this.preGameNode = EventNode.event(creationInfo.gameId().toString(), EventFilter.ALL, GameEventPredicates.inCreationInfo(creationInfo));
        GameEventNodes.PRE_GAME.addChild(this.preGameNode);

        this.preGameNode.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            game.getPlayers().add(event.getPlayer());
            game.onPreJoin(event.getPlayer());

            event.setSpawningInstance(game.getSpawningInstance(event.getPlayer()));
        });

        this.preGameNode.addListener(PlayerDisconnectEvent.class, event -> {
            game.getPlayers().remove(event.getPlayer());
            game.onLeave(event.getPlayer());

            if (game.getPlayers().isEmpty()) {
                game.finish();
            }
        });

        this.preGameNode.addListener(PlayerSpawnEvent.class, event -> {
            game.onJoin(event.getPlayer());

            int newCount = this.playerCount.incrementAndGet();
            if (newCount != creationInfo.playerIds().size()) return;

            gameManager.startGame(game);
            if (this.startTimeOutTask != null) this.startTimeOutTask.cancel();
        });

        this.preGameNode.addListener(GameFinishedEvent.class, event -> {
            if (!event.game().equals(game)) return;
            GameEventNodes.PRE_GAME.removeChild(this.preGameNode);
        });

        this.startTimeOutTask = MinecraftServer.getSchedulerManager().buildTask(this::timeOut).delay(10, ChronoUnit.SECONDS).schedule();
    }

    private void timeOut() {
        int actualPlayerCount = this.game.getPlayers().size();
        if (actualPlayerCount >= this.info.minPlayers()) {
            this.gameManager.startGame(this.game);
        } else {
            if (this.startTimeOutTask != null) this.startTimeOutTask.cancel();
            GameEventNodes.PRE_GAME.removeChild(this.preGameNode);

            // TODO: inform players that the game couldn't start and requeue them
            this.game.finish();
        }
    }
}
