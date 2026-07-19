package dev.emortal.minestom.core.game;

import dev.emortal.messaging.message.Channel;
import dev.emortal.messaging.message.SendLobbyMessage;
import dev.emortal.minestom.core.EmortalServer;
import dev.emortal.minestom.core.game.config.GameConfig;
import dev.emortal.minestom.core.game.config.GameCreationInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class GameManager implements GameProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameManager.class);

    private final @NotNull GameConfig config;

    private final Set<Game> games = ConcurrentHashMap.newKeySet();

    public GameManager(@NotNull GameConfig config) {
        this.config = config;

        GameEventNodes.GAME_MANAGER.addListener(GameFinishedEvent.class, this::onGameFinish);
    }

    public @NotNull Game createGame(@NotNull GameCreationInfo creationInfo) {
        Game game = this.config.gameCreator().createGame(creationInfo);
        this.registerGame(game);
        return game;
    }

    private void registerGame(@NotNull Game game) {
        boolean added = this.games.add(game);
        if (!added) {
            LOGGER.warn("Attempted to add game {} that is already registered", game);
            return;
        }
        GameEventNodes.GAMES.addChild(game.getEventNode());
    }

    public void startGame(@NotNull Game game) {
        LOGGER.info("Starting game {}", game.getCreationInfo());
        game.start();
    }

    private void removeGame(@NotNull Game game) {
        boolean removed = this.games.remove(game);
        if (!removed) {
            LOGGER.warn("Attempted to remove game {} that is not registered", game);
            return;
        }
        GameEventNodes.GAMES.removeChild(game.getEventNode());
    }

    private void onGameFinish(@NotNull GameFinishedEvent event) {
        Game game = event.game();
        if (!this.games.contains(game)) {
            // Definitely don't want a double remove and clean up
            LOGGER.info("Game {} already finished and removed. Ignoring finish request.", game.getCreationInfo());
            return;
        }

        LOGGER.info("Game {} finished", game.getCreationInfo());

        this.removeGame(game);
//        switch (this.config.finishBehaviour()) {
//            case LOBBY -> Entrypoint.getRedis().sendToLobby(playersToUuids(game.getPlayers()));
//            case REQUEUE -> {} // TODO: this
//        }
        EmortalServer.getRedis().sendMessage(Channel.PROXY, new SendLobbyMessage(playersToUuids(game.getPlayers())));

        MinecraftServer.getSchedulerManager().buildTask(() -> this.cleanUpGame(game))
                .delay(TaskSchedule.tick(20 * 5))
                .schedule();
    }

    private List<UUID> playersToUuids(Collection<Player> players) {
        List<UUID> uuids = new ArrayList<>();
        for (Player player : players) {
            uuids.add(player.getUuid());
        }
        return uuids;
    }

    private void cleanUpGame(@NotNull Game game) {
        LOGGER.info("Cleaning up game {}", game.getCreationInfo());
        this.kickAllRemainingPlayers(game);
        game.cleanUp();
    }

    private void kickAllRemainingPlayers(@NotNull Game game) {
        for (Player player : game.getPlayers()) {
            // Don't kick players that aren't online
            if (!player.isOnline()) continue;

            // The player may have been moved to a different game on the same server
            if (player.getInstance() != game.getSpawningInstance(player)) continue;

            player.kick(Component.text("The game ended but we weren't able to connect you to a lobby. Please reconnect.", NamedTextColor.RED));
        }
    }

    @Override
    public @Nullable Game findGame(@NotNull Player player) {
        for (Game game : this.games) {
            if (game.getPlayers().contains(player)) return game;
        }
        return null;
    }

    @Override
    public int getGameCount() {
        return this.games.size();
    }

    public @NotNull Set<Game> getGames() {
        return this.games;
    }
}
