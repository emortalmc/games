package dev.emortal.minestom.lobby;

import com.alibaba.fastjson2.JSON;
import dev.emortal.messaging.message.Channel;
import dev.emortal.messaging.message.ProxyOnlineMessage;
import dev.emortal.messaging.message.ServerOnlineMessage;
import dev.emortal.messaging.types.GameInfo;
import dev.emortal.minestom.core.EmortalServer;
import dev.emortal.minestom.lobby.blockhandler.SignHandler;
import dev.emortal.minestom.lobby.commands.SpawnCommand;
import dev.emortal.minestom.lobby.commands.TrainCommand;
import dev.emortal.minestom.lobby.config.GameModesConfig;
import dev.emortal.minestom.lobby.emote.Emote;
import dev.emortal.minestom.lobby.features.*;
import dev.emortal.minestom.lobby.game.ServerSelector;
import dev.emortal.minestom.lobby.util.PolarConvertingLoader;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockManager;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Entrypoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(Entrypoint.class);

    private static final Path GAMES_CONFIG_PATH = Path.of("games.json");

    private static final int SPAWN_CHUNK_RADIUS = 5;
    public static final Pos SPAWN_POINT = new Pos(0.5, 66, 0.5, 180f, 0f);

    static void main() {
        EmortalServer.start(() -> {
            registerSignHandlers();
            Emote.init(MinecraftServer.getGlobalEventHandler());

            MinecraftServer.getTeamManager().createBuilder("npcTeam")
                    .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
                    .updateTeamPacket()
                    .build();

            PolarConvertingLoader loader = new PolarConvertingLoader("lobby");
            Instance instance = loader.load().join();

            instance.enableAutoChunkLoad(false);
            for (int x = -SPAWN_CHUNK_RADIUS; x < SPAWN_CHUNK_RADIUS; x++) {
                for (int y = -SPAWN_CHUNK_RADIUS; y < SPAWN_CHUNK_RADIUS; y++) {
                    instance.loadChunk(x, y);
                }
            }

            EventNode<Event> eventNode = MinecraftServer.getGlobalEventHandler();

            LobbyEvents.registerGeneric(eventNode, instance);
            LobbyEvents.registerProtectionEvents(eventNode, instance);

            CommandManager commandManager = MinecraftServer.getCommandManager();
            commandManager.register(new SpawnCommand(instance));
            commandManager.register(new TrainCommand(instance));

            spawnFeatures(instance);

            byte[] jsonBytes;
            try {
                jsonBytes = Files.readAllBytes(GAMES_CONFIG_PATH);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            GameModesConfig gameModesConfig = JSON.parseObject(jsonBytes, GameModesConfig.class);

            new ServerSelector(instance, eventNode, gameModesConfig.gamemodes());
        });

        String publicAddress = getValue("publicAddress", "localhost");
        int port = Integer.parseInt(getValue("port", "25565"));
        GameInfo gameInfo = new GameInfo("lobby", List.of(), 1, 50, GameInfo.MatchMethod.INSTANT);
        ServerOnlineMessage onlineMessage = new ServerOnlineMessage(EmortalServer.getServerUuid(), publicAddress, port, List.of(gameInfo));
        EmortalServer.getRedis().sendMessage(Channel.PROXY, onlineMessage);

        EmortalServer.getRedis().addMessageHandler(ProxyOnlineMessage.class, (_, _) -> {
            EmortalServer.getRedis().sendMessage(Channel.PROXY, onlineMessage);
            LOGGER.info("Proxy online, sent online message");
        });

    }

    private static @NotNull String getValue(@NotNull String key, @NotNull String defaultValue) {
        String value = System.getProperty(key);
        if (value != null && !value.isEmpty()) return value;

        return defaultValue;
    }

    private static void registerSignHandlers() {
        BlockManager blockManager = MinecraftServer.getBlockManager();
        blockManager.registerHandler("minecraft:sign", SignHandler::new);

        for (Block value : Block.values()) {
            if (value.name().endsWith("sign")) blockManager.registerHandler(value.key(), SignHandler::new);
        }
    }

    private static void spawnFeatures(@NotNull Instance instance) {
        new ArmorStandsFeature().register(instance);
        new GraffitiFeature().register(instance);
        new SpinnyCubeFeature().register(instance);
        new RamMsptIndicatorFeature().register(instance);
        new GregoryFeature().register(instance);
        new HotelReceptionistFeature().register(instance);
        new HotelLiftFeature().register(instance);
        new NewsReporterFeature().register(instance);
//        new SadMattFeature().register(instance);
        new RavenousFeature().register(instance);
        new SecretLeverFeature().register(instance);
        new OldLobbyFeature().register(instance);
        new LightsOutFeature().register(instance);
        new ClickySignFeature().register(instance);
        new SeatingFeature().register(instance);
        new ThrowingFeature().register(instance);
        new TelephoneWiresFeature().register(instance);
        new EmortalRoomFeature().register(instance);
        new ABSSecretFeature().register(instance);
        new ModelDecorationFeature().register(instance);
    }

}
