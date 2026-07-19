package dev.emortal.minestom.marathon;

import dev.emortal.messaging.types.GameInfo;
import dev.emortal.messaging.types.MarathonData;
import dev.emortal.minestom.core.EmortalServer;
import dev.emortal.minestom.core.game.config.GameConfig;
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
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Main {
    public static final @NotNull MarathonData DEFAULT_PLAYER_DATA = new MarathonData(Time.MIDNIGHT.name(), BlockPalette.OVERWORLD.name(), BlockAnimation.POPOUT.name());

    private static @Nullable LeaderboardDB DB;

    static void main() {
        EmortalServer.start(() -> {
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
            RegistryKey<DimensionType> dimension = MinecraftServer.getDimensionTypeRegistry().register(Key.key("fullbright"), dimensionType);

            GameConfig gameConfig = new GameConfig(1, GameConfig.FinishBehaviour.LOBBY, info -> {
                Map<UUID, MarathonData> playerData = new HashMap<>();
                for (UUID uuid : info.playerIds()) {
                    MarathonData data = null;
                    if (DB != null) data = DB.getSettings(uuid);
                    if (data == null) data = DEFAULT_PLAYER_DATA;
                    playerData.put(uuid, data);
                }

                return new MarathonGameRunner(info, dimension, playerData);
            });
            GameInfo gameInfo = new GameInfo("marathon", List.of(), 1, 1, GameInfo.MatchMethod.INSTANT);
            EmortalServer.registerGame(gameInfo, gameConfig);

            String dbConnString = getValue("dbUrl", "");
            String dbUserString = getValue("dbUser", "");
            String dbPassString = getValue("dbPass", "");
            if (!dbConnString.isBlank()) {
                DB = new LeaderboardDB(dbConnString, dbUserString, dbPassString);
                DB.connect();
                MinecraftServer.getCommandManager().register(new LeaderboardCommand(DB));
            }
        });
    }

    public static @Nullable LeaderboardDB getLeaderboardDB() {
        return DB;
    }

    private static @NotNull String getValue(@NotNull String key, @NotNull String defaultValue) {
        String value = System.getProperty(key);
        if (value != null && !value.isEmpty()) return value;

        return defaultValue;
    }
}
