package dev.emortal.minestom.marathon;

import dev.emortal.messaging.types.GameInfo;
import dev.emortal.messaging.types.MarathonData;
import dev.emortal.minestom.core.EmortalServer;
import dev.emortal.minestom.core.Module;
import dev.emortal.minestom.core.game.GameCreator;
import dev.emortal.minestom.core.game.config.GameConfig;
import dev.emortal.minestom.core.map.MapManager;
import dev.emortal.minestom.marathon.command.LeaderboardCommand;
import dev.emortal.minestom.marathon.leaderboard.LeaderboardDB;
import dev.emortal.minestom.marathon.options.BlockAnimation;
import dev.emortal.minestom.marathon.options.BlockPalette;
import dev.emortal.minestom.marathon.options.Time;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.color.Color;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.attribute.EnvironmentAttribute;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MarathonModule implements Module {
    public static final @NotNull MarathonData DEFAULT_PLAYER_DATA = new MarathonData(Time.MIDNIGHT.name(), BlockPalette.OVERWORLD.name(), BlockAnimation.POPOUT.name());

    private LeaderboardDB db;
    private RegistryKey<DimensionType> dimension;

    private void connectToLeaderboardDb() {
        String dbConnString = EmortalServer.getValue("DB_URL", "");
        String dbUserString = EmortalServer.getValue("DB_USER", "");
        String dbPassString = EmortalServer.getValue("DB_PASS", "");
        if (!dbConnString.isBlank()) {
            this.db = new LeaderboardDB(dbConnString, dbUserString, dbPassString);
            db.connect();
            MinecraftServer.getCommandManager().register(new LeaderboardCommand(db));
        }
    }

    @Override
    public String getId() {
        return "marathon";
    }

    @Override
    public int getMinPlayers() {
        return 1;
    }

    @Override
    public int getMaxPlayers() {
        return 1;
    }

    @Override
    public GameInfo.MatchMethod getMatchMethod() {
        return GameInfo.MatchMethod.INSTANT;
    }

    @Override
    public GameConfig.FinishBehaviour getFinishBehaviour() {
        return GameConfig.FinishBehaviour.REQUEUE;
    }

    @Override
    public GameCreator getGameCreator() {
        return (info, map) -> {
            Map<UUID, MarathonData> playerData = new HashMap<>();
            for (UUID uuid : info.playerIds()) {
                if (db == null) continue;
                MarathonData settings = db.getSettings(uuid);
                if (settings == null) continue;
                playerData.put(uuid, settings);
            }

            return new MarathonGameRunner(info, dimension, playerData, db);
        };
    }

    @Override
    public MapManager getMapManager() {
        DimensionType overworld = MinecraftServer.getDimensionTypeRegistry().get(DimensionType.OVERWORLD);
        DimensionType dimensionType = DimensionType.builder()
                .timelines(overworld.timelines())
                .setAttribute(EnvironmentAttribute.CLOUD_COLOR, ShadowColor.fromHexString("#ffffffcc"))
                .setAttribute(EnvironmentAttribute.FOG_COLOR, new Color(0xc0d8ff))
                .setAttribute(EnvironmentAttribute.SKY_COLOR, new Color(0x78a7ff))
                .setAttribute(EnvironmentAttribute.CLOUD_HEIGHT, 110f)
                .ambientLight(1f)
                .setAttribute(EnvironmentAttribute.AMBIENT_LIGHT_COLOR, NamedTextColor.WHITE)
                .defaultClock(overworld.defaultClock())
                .build();
        this.dimension = MinecraftServer.getDimensionTypeRegistry().register(Key.key("fullbright"), dimensionType);

        return new MarathonMapManager();
    }

    @Override
    public void preRegister() {
        connectToLeaderboardDb();
    }
}
