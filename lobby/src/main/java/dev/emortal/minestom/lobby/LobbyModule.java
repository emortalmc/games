package dev.emortal.minestom.lobby;

import dev.emortal.api.liveconfigparser.configs.ConfigProvider;
import dev.emortal.api.liveconfigparser.configs.gamemode.GameModeConfig;
import dev.emortal.api.modules.annotation.Dependency;
import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.api.modules.env.ModuleEnvironment;
import dev.emortal.api.service.matchmaker.MatchmakerService;
import dev.emortal.api.service.playertracker.PlayerTrackerService;
import dev.emortal.api.utils.GrpcStubCollection;
import dev.emortal.minestom.core.module.MinestomModule;
import dev.emortal.minestom.core.module.kubernetes.KubernetesModule;
import dev.emortal.minestom.core.module.liveconfig.LiveConfigModule;
import dev.emortal.minestom.core.module.messaging.MessagingModule;
import dev.emortal.minestom.lobby.blockhandler.SignHandler;
import dev.emortal.minestom.lobby.commands.SpawnCommand;
import dev.emortal.minestom.lobby.commands.TrainCommand;
import dev.emortal.minestom.lobby.emote.Emote;
import dev.emortal.minestom.lobby.events.EventManager;
import dev.emortal.minestom.lobby.features.*;
import dev.emortal.minestom.lobby.util.PolarConvertingLoader;
import net.hollowcube.polar.ChunkSelector;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockManager;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.stream.Collectors;

@ModuleData(name = "lobby", dependencies = {@Dependency(name = "live-config"), @Dependency(name = "kubernetes", required = false), @Dependency(name = "messaging", required = false)})
public final class LobbyModule extends MinestomModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(LobbyModule.class);

    public static final Pos SPAWN_POINT = new Pos(0.5, 66, 0.5, 180f, 0f);
    private static final int SPAWN_CHUNK_RADIUS = 5;

    @Nullable MatchmakerService matchmaker;
    @Nullable PlayerTrackerService playerTracker;
    ConfigProvider<GameModeConfig> configProvider;

    LobbyModule(@NotNull ModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        this.registerSignHandlers();
        Emote.init(MinecraftServer.getGlobalEventHandler());

        MinecraftServer.getTeamManager().createBuilder("npcTeam")
                .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
                .updateTeamPacket()
                .build();

        PolarConvertingLoader loader = new PolarConvertingLoader("lobby", ChunkSelector.radius(SPAWN_CHUNK_RADIUS));
        Instance instance = loader.load().join();

        instance.enableAutoChunkLoad(false);
        for (int x = -SPAWN_CHUNK_RADIUS; x < SPAWN_CHUNK_RADIUS; x++) {
            for (int y = -SPAWN_CHUNK_RADIUS; y < SPAWN_CHUNK_RADIUS; y++) {
                instance.loadChunk(x, y);
            }
        }

        this.spawnFeatures(instance);

        LiveConfigModule liveConfigModule = this.environment.moduleProvider().getModule(LiveConfigModule.class);

        if (liveConfigModule != null) {
            this.configProvider = liveConfigModule.getGameModes();

            if (this.configProvider == null) {
                LOGGER.warn("GameModeCollection is not present in LiveConfigModule");
            } else {
                Collection<GameModeConfig> allConfigs = this.configProvider.allConfigs();
                LOGGER.info("Loaded modes ({}): {}", allConfigs.size(), allConfigs.stream().map(GameModeConfig::friendlyName).collect(Collectors.joining(", ")));
                LOGGER.debug("Game modes: {}", allConfigs);
                this.matchmaker = GrpcStubCollection.getMatchmakerService().orElse(null);
                this.playerTracker = GrpcStubCollection.getPlayerTrackerService().orElse(null);
            }
        }

        MessagingModule messagingModule = this.environment.moduleProvider().getModule(MessagingModule.class);

        if (messagingModule != null) GrpcStubCollection.getPartyService().ifPresent(partyService -> {
            EventNode<@NotNull PlayerEvent> eventManagerNode = EventNode.type("event-manager", EventFilter.PLAYER);
            MinecraftServer.getGlobalEventHandler().addChild(eventManagerNode);
            new EventManager(messagingModule, partyService, eventManagerNode, instance);
        });

        LobbyEvents.registerGeneric(this, this.eventNode, instance);
        LobbyEvents.registerProtectionEvents(this.eventNode, instance);

        CommandManager commandManager = MinecraftServer.getCommandManager();
        commandManager.register(new SpawnCommand(instance));
        commandManager.register(new TrainCommand(instance));

        this.loadKubernetesFeatures();
        return true;
    }

    private void spawnFeatures(@NotNull Instance instance) {
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

    private void loadKubernetesFeatures() {
        KubernetesModule kubernetes = this.getOptionalModule(KubernetesModule.class);
        if (kubernetes == null || kubernetes.getAgonesSdk() == null) return;

        new PlayerDisconnectHandler(kubernetes.getAgonesSdk());
    }

    @Override
    public void onUnload() {
    }

    private void registerSignHandlers() {
        BlockManager blockManager = MinecraftServer.getBlockManager();
        blockManager.registerHandler("minecraft:sign", SignHandler::new);

        for (Block value : Block.values()) {
            if (value.name().endsWith("sign")) blockManager.registerHandler(value.key(), SignHandler::new);
        }
    }
}