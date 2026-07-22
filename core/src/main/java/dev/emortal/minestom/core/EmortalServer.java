package dev.emortal.minestom.core;

import dev.emortal.messaging.RedisMessenger;
import dev.emortal.messaging.message.*;
import dev.emortal.messaging.types.GameInfo;
import dev.emortal.minestom.core.command.game.CreditsCommand;
import dev.emortal.minestom.core.command.game.StopCommand;
import dev.emortal.minestom.core.game.Game;
import dev.emortal.minestom.core.game.GameManager;
import dev.emortal.minestom.core.game.PreGameInitializer;
import dev.emortal.minestom.core.game.config.GameConfig;
import dev.emortal.minestom.core.game.config.GameCreationInfo;
import dev.emortal.minestom.core.utils.command.ExtraConditions;
import me.lucko.spark.minestom.SparkMinestom;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

public final class EmortalServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmortalServer.class);

    private static final String DEFAULT_ADDRESS = "localhost";
    private static final String DEFAULT_PORT = "25565";
    private static final String DEFAULT_REDIS_ADDRESS = "redis://localhost";

    private static final UUID SERVER_UUID = UUID.randomUUID();

    private static GameManager GAME_MANAGER;
    private static RedisMessenger REDIS;
    private static PermissionHandler PERMISSIONS;
    private static SparkMinestom SPARK;

    private static final Map<String, GameConfig> GAME_MAP = new HashMap<>();
    private static final Map<String, GameInfo> GAME_INFO_MAP = new HashMap<>();

    public static void registerGame(GameInfo gameInfo, GameConfig config) {
        String id = gameInfo.gameId();
        GAME_MAP.put(id, config);
        GAME_INFO_MAP.put(id, gameInfo);
    }

    public static void start(Module... modules) {
        start(List.of(modules));
    }

    public static void start(Collection<Module> modules) {
        String address = getValue("address", DEFAULT_ADDRESS);
        String publicAddress = getValue("publicAddress", DEFAULT_ADDRESS);
        String velocitySecret = getValue("velocitySecret", "");
        String redisAddress = getValue("redisAddress", DEFAULT_REDIS_ADDRESS);
        boolean onlineMode = Boolean.parseBoolean(getValue("online", "false"));
        int port = Integer.parseInt(getValue("port", DEFAULT_PORT));

        REDIS = new RedisMessenger(redisAddress);
        REDIS.listenForServerUUID(SERVER_UUID);

        Auth auth;
        if (velocitySecret.isBlank()) {
            if (onlineMode) auth = new Auth.Online();
            else auth = new Auth.Offline();
            LOGGER.info("Starting server at {}:{}, online mode: {}", address, port, onlineMode);
        } else {
            auth = new Auth.Velocity(velocitySecret);
            LOGGER.info("Starting server at {}:{} using Velocity forwarding", address, port);
            MinecraftServer.setCompressionThreshold(0);
        }

        LOGGER.info("Server UUID: {}", SERVER_UUID);

        MinecraftServer server = MinecraftServer.init(auth);

        GAME_MANAGER = new GameManager();
        PERMISSIONS = new PermissionHandler(REDIS, MinecraftServer.getGlobalEventHandler());

        for (Module module : modules) {
            module.register();
        }

        Path directory = Path.of("spark");
        SPARK = SparkMinestom.builder(directory)
                .commands(true)
                .permissionHandler(ExtraConditions::hasPermission)
                .enable();

        MinecraftServer.getCommandManager().register(new StopCommand());
        MinecraftServer.getCommandManager().register(new CreditsCommand());

        MinestomTerminal.start();

        server.start(address, port);

        if (!GAME_MAP.isEmpty()) {
            ServerOnlineMessage onlineMessage = new ServerOnlineMessage(SERVER_UUID, publicAddress, port, GAME_INFO_MAP.values());
            REDIS.sendMessage(Channel.PROXY, onlineMessage);
            LOGGER.info("Sent online message");

            REDIS.addMessageHandler(ProxyOnlineMessage.class, (_, _) -> {
                REDIS.sendMessage(Channel.PROXY, onlineMessage);
                LOGGER.info("Proxy back online, sent online message");
            });

            REDIS.addMessageHandler(CreateGameMessage.class, (channel, msg) -> {
                GameManager gameManager = getGameManager();

                GameInfo gameInfo = getGameInfo(msg.gameId());
                if (gameInfo == null) {
                    LOGGER.error("No game info for game " + msg.gameId());
                    return;
                }

                Game game = gameManager.createGame(new GameCreationInfo(msg.gameId(), msg.map(), new HashSet<>(msg.players())));

                new PreGameInitializer(gameManager, gameInfo, game);

                REDIS.sendMessage(Channel.PROXY, new GameReadyMessage(SERVER_UUID));
            });
        }
    }

    public static void stop() {
        SPARK.shutdown();

        LOGGER.info("Finishing all games");
        for (Game game : GAME_MANAGER.getGames()) {
            game.finish();
        }

        MinecraftServer.getSchedulerManager().buildTask(() -> {
            LOGGER.info("Stopping server");
            MinestomTerminal.stop();
            MinecraftServer.getSchedulerManager().scheduleEndOfTick(MinecraftServer::stopCleanly);
        }).delay(TaskSchedule.tick(6 * 20)).schedule();
    }

    public static RedisMessenger getRedis() {
        return REDIS;
    }

    public static boolean hasPermission(Player player, String permission) {
        return PERMISSIONS.hasPermission(player, permission);
    }

    public static UUID getServerUuid() {
        return SERVER_UUID;
    }

    public static @NotNull GameManager getGameManager() {
        return GAME_MANAGER;
    }
    public static @Nullable GameConfig getGameConfig(String gameId) {
        return GAME_MAP.get(gameId);
    }
    public static @Nullable GameInfo getGameInfo(String gameId) {
        return GAME_INFO_MAP.get(gameId);
    }

    public static @NotNull String getValue(@NotNull String key, @NotNull String defaultValue) {
        String value = System.getProperty(key);
        if (value != null && !value.isEmpty()) return value;

        return defaultValue;
    }

}
