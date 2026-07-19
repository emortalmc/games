package dev.emortal.minestom.blocksumo;

import dev.emortal.messaging.types.BlockSumoData;
import dev.emortal.messaging.types.GameInfo;
import dev.emortal.minestom.blocksumo.command.GameCommand;
import dev.emortal.minestom.blocksumo.command.SaveLoadoutCommand;
import dev.emortal.minestom.blocksumo.game.BlockSumoGame;
import dev.emortal.minestom.core.EmortalServer;
import dev.emortal.minestom.core.command.game.CreditsCommand;
import dev.emortal.minestom.core.game.GameManager;
import dev.emortal.minestom.core.game.config.GameConfig;
import dev.emortal.minestom.core.map.MapManager;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Main {
    public static final @NotNull BlockSumoData DEFAULT_PLAYER_DATA = new BlockSumoData(0, 45);
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 16;
    private static final Set<String> MAPS = Set.of(
            "blocksumo",
            "castle",
            "end",
            "ice",
            "ruins",
            "deepdark"
    );

    void main() {
        EmortalServer.start(() -> {

            MapManager mapManager = new MapManager(MAPS);

            GameConfig gameConfig = new GameConfig(MIN_PLAYERS, GameConfig.FinishBehaviour.LOBBY, info -> {
                Map<UUID, BlockSumoData> playerData = new HashMap<>();
                for (UUID uuid : info.playerIds()) {
                    playerData.put(uuid, DEFAULT_PLAYER_DATA); // TODO: this
                }

                return new BlockSumoGame(info, mapManager.loadMap(info.map()), playerData);
            });
            GameInfo gameInfo = new GameInfo("blocksumo", MAPS, MIN_PLAYERS, MAX_PLAYERS, GameInfo.MatchMethod.COUNTDOWN);
            GameManager gameManager = EmortalServer.registerGame(gameInfo, gameConfig);

            MinecraftServer.getCommandManager().register(new GameCommand(gameManager));
            MinecraftServer.getCommandManager().register(new CreditsCommand(gameManager));
            MinecraftServer.getCommandManager().register(new SaveLoadoutCommand(gameManager));
        });
    }
}
