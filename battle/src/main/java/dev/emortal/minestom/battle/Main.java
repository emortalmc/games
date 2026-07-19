package dev.emortal.minestom.battle;

import dev.emortal.messaging.types.GameInfo;
import dev.emortal.minestom.battle.game.BattleGame;
import dev.emortal.minestom.core.EmortalServer;
import dev.emortal.minestom.core.game.config.GameConfig;
import dev.emortal.minestom.core.map.MapManager;
import io.github.togar2.pvp.MinestomPvP;

import java.util.Set;

public final class Main {
    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 12;
    private static final Set<String> MAPS = Set.of(
            "caverns",
            "cove",
            "crucible"
    );

    static void main() {
        EmortalServer.start(() -> {
            MinestomPvP.init();

            MapManager mapManager = new MapManager(MAPS);
            GameInfo gameInfo = new GameInfo("battle", MAPS, MIN_PLAYERS, MAX_PLAYERS, GameInfo.MatchMethod.COUNTDOWN);
            GameConfig gameConfig = new GameConfig(MIN_PLAYERS, GameConfig.FinishBehaviour.LOBBY, info -> new BattleGame(info, mapManager.loadMap(info.map())));
            EmortalServer.registerGame(gameInfo, gameConfig);
        });
    }
}