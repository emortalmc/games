package dev.emortal.minestom.lazertag;

import dev.emortal.messaging.types.GameInfo;
import dev.emortal.minestom.core.EmortalServer;
import dev.emortal.minestom.core.command.game.CreditsCommand;
import dev.emortal.minestom.core.game.GameManager;
import dev.emortal.minestom.core.game.config.GameConfig;
import dev.emortal.minestom.core.map.MapManager;
import dev.emortal.minestom.lazertag.game.LazerTagGame;
import net.minestom.server.MinecraftServer;

import java.util.Set;

public final class Main {
    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 20;
    private static final Set<String> MAPS = Set.of("dizzymc");

    void main() {
        EmortalServer.start(() -> {
            MapManager mapManager = new MapManager(MAPS);
            GameConfig gameConfig = new GameConfig(MIN_PLAYERS, GameConfig.FinishBehaviour.LOBBY, info -> new LazerTagGame(info, mapManager.loadMap(info.map())));
            GameInfo gameInfo = new GameInfo("lazertag", MAPS, MIN_PLAYERS, MAX_PLAYERS, GameInfo.MatchMethod.COUNTDOWN);
            GameManager gameManager = EmortalServer.registerGame(gameInfo, gameConfig);

            MinecraftServer.getCommandManager().register(new CreditsCommand(gameManager));
        });
    }
}
