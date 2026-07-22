package dev.emortal.minestom.lobby;

import com.alibaba.fastjson2.JSON;
import dev.emortal.messaging.types.GameInfo;
import dev.emortal.minestom.core.Module;
import dev.emortal.minestom.core.game.GameCreator;
import dev.emortal.minestom.core.game.config.GameConfig;
import dev.emortal.minestom.core.map.MapManager;
import dev.emortal.minestom.core.utils.PolarUtil;
import dev.emortal.minestom.lobby.blockhandler.SignHandler;
import dev.emortal.minestom.lobby.commands.SpawnCommand;
import dev.emortal.minestom.lobby.commands.TrainCommand;
import dev.emortal.minestom.lobby.config.GameModesConfig;
import dev.emortal.minestom.lobby.emote.Emote;
import dev.emortal.minestom.lobby.features.*;
import dev.emortal.minestom.lobby.game.ServerSelector;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockManager;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LobbyModule implements Module {

    public static final Pos SPAWN_POINT = new Pos(0.5, 66, 0.5, 180f, 0f);
    private static final Path GAMES_CONFIG_PATH = Path.of("games.json");

    private InstanceContainer instance;

    private InstanceContainer createInstance() {
        this.instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.enableAutoChunkLoad(false);
        Path path = Path.of("lobby.polar");
        PolarUtil.stream(instance, path).join();
        return instance;
    }

    @Override
    public String getId() {
        return "lobby";
    }

    @Override
    public int getMinPlayers() {
        return 1;
    }

    @Override
    public int getMaxPlayers() {
        return 50;
    }

    @Override
    public GameCreator getGameCreator() {
        return LobbyGame::new;
    }

    @Override
    public MapManager getMapManager() {
        return new LobbyMapManager(createInstance());
    }

    @Override
    public GameConfig.FinishBehaviour getFinishBehaviour() {
        return GameConfig.FinishBehaviour.REQUEUE;
    }

    @Override
    public GameInfo.MatchMethod getMatchMethod() {
        return GameInfo.MatchMethod.INSTANT;
    }

    @Override
    public void preRegister() {
        registerSignHandlers();
    }

    @Override
    public void postRegister() {
        Emote.init(MinecraftServer.getGlobalEventHandler());

        MinecraftServer.getTeamManager().createBuilder("npcTeam")
                .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
                .updateTeamPacket()
                .build();

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
