package dev.emortal.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import dev.emortal.messaging.RedisMessenger;
import dev.emortal.messaging.message.Channel;
import dev.emortal.messaging.message.ProxyOnlineMessage;
import dev.emortal.messaging.message.ServerOnlineMessage;
import dev.emortal.messaging.types.GameInfo;
import dev.emortal.velocity.command.DiscordCommand;
import dev.emortal.velocity.command.LobbyCommand;
import dev.emortal.velocity.command.UpdateResourcePack;
import dev.emortal.velocity.resourcepack.ResourcePackSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Plugin(id = "core", name = "Core", dependencies = { @Dependency(id = "luckperms") })
public final class CorePlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorePlugin.class);

    private final Map<String, List<UUID>> gameIdMap = new HashMap<>();
    private final Map<UUID, Collection<GameInfo>> supportedGamesMap = new HashMap<>();
    private final Map<String, GameInfo> gameInfoMap = new HashMap<>();

    private final @NotNull ProxyServer proxy;
    private final RedisMessenger redis;
    private final Matchmaker matchmaker;

    private ResourcePackSender rpSender;

    @Inject
    public CorePlugin(@NotNull ProxyServer server) {
        this.proxy = server;

        String redisAddress = getValue("redisAddress", "redis://localhost");
        this.redis = new RedisMessenger(redisAddress);
        this.matchmaker = new Matchmaker(this, redis);
    }

    @Subscribe
    public void onProxyInitialize(@NotNull ProxyInitializeEvent event) {
        this.rpSender = new ResourcePackSender(this.proxy);

        this.proxy.getEventManager().register(this, this.rpSender);
        this.proxy.getEventManager().register(this, this.matchmaker);
        this.proxy.getEventManager().register(this, new ServerPingListener());
        this.proxy.getEventManager().register(this, new LunarListener());
        this.proxy.getEventManager().register(this, new ServerListener(this, this.proxy));
        this.proxy.getEventManager().register(this, new PermissionsListener(this));

        this.redis.listenForChannel(Channel.PROXY);
        this.redis.sendMessage(Channel.ALL, new ProxyOnlineMessage());
        this.redis.addMessageHandler(ServerOnlineMessage.class, (channel, msg) -> {
            for (GameInfo gameInfo : msg.games()) {
                List<UUID> uuids = gameIdMap.computeIfAbsent(gameInfo.gameId(), _ -> new ArrayList<>());
                uuids.add(msg.serverId());
                gameInfoMap.put(gameInfo.gameId(), gameInfo);
            }
            supportedGamesMap.put(msg.serverId(), msg.games());

            this.proxy.registerServer(new ServerInfo(msg.serverId().toString(), new InetSocketAddress(msg.address(), msg.port())));
            LOGGER.info("Registered server " + msg.serverId());
        });

        // automatically unregister offline servers
        this.proxy.getScheduler().buildTask(this, () -> {
            for (RegisteredServer allServer : proxy.getAllServers()) {
                try {
                    allServer.ping().get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    LOGGER.info("Unregistering " + allServer.getServerInfo().getName() + " as it went offline");
                    handleServerTimeout(allServer.getServerInfo());
                }
            }
        }).repeat(10, TimeUnit.SECONDS).schedule();

        registerCommands();
    }

    private void handleServerTimeout(ServerInfo server) {
        proxy.unregisterServer(server);
        UUID uuid = UUID.fromString(server.getName());

        for (List<UUID> value : gameIdMap.values()) {
            value.remove(uuid);
        }
        supportedGamesMap.remove(uuid);
    }


    public @NotNull ProxyServer getProxy() {
        return proxy;
    }

    public RedisMessenger getRedis() {
        return redis;
    }

    public Matchmaker getMatchmaker() {
        return matchmaker;
    }

    public Map<UUID, Collection<GameInfo>> getSupportedGamesMap() {
        return supportedGamesMap;
    }

    public @Nullable UUID getServerUUID(String gameId) {
        List<UUID> uuids = gameIdMap.get(gameId);
        if (uuids == null || uuids.isEmpty()) return null;

        return uuids.getFirst();
    }

    public @Nullable RegisteredServer getServer(String gameId) {
        UUID uuid = getServerUUID(gameId);
        if (uuid == null) return null;
        return proxy.getServer(uuid.toString()).orElse(null);
    }

    public @Nullable GameInfo getGameInfo(String gameId) {
        return gameInfoMap.get(gameId);
    }

    @Subscribe
    public void onProxyShutdown(@NotNull ProxyShutdownEvent event) {

    }

    private void registerCommands() {
        CommandManager commandManager = proxy.getCommandManager();

        // TODO: playtime command
        // TODO: msg, reply command
        // TODO: parties

        DiscordCommand.registerCommand(this, commandManager);
        LobbyCommand.registerCommand(this, commandManager);
        UpdateResourcePack.registerCommand(this, commandManager, rpSender);
    }

    private static @NotNull String getValue(@NotNull String key, @NotNull String defaultValue) {
        String value = System.getProperty(key);
        if (value != null && !value.isEmpty()) return value;

        return defaultValue;
    }
}
