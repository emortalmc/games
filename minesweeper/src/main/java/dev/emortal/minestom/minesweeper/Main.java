package dev.emortal.minestom.minesweeper;

import dev.emortal.messaging.types.GameInfo;
import dev.emortal.minestom.core.EmortalServer;
import dev.emortal.minestom.core.game.config.GameConfig;
import dev.emortal.minestom.minesweeper.board.Board;
import dev.emortal.minestom.minesweeper.game.MinesweeperGame;
import dev.emortal.minestom.minesweeper.map.MapManager;

import java.util.List;

public final class Main {
    private static final int MIN_PLAYERS = 1;

    void main() {
        EmortalServer.start(() -> {
            MapManager mapManager = new MapManager();
            mapManager.registerDimensions();

            GameConfig gameConfig = new GameConfig(MIN_PLAYERS, GameConfig.FinishBehaviour.REQUEUE, info -> {
                Board board = mapManager.getOrCreateMap(info.playerIds());
                return new MinesweeperGame(info, board);
            });
            GameInfo gameInfo = new GameInfo("minesweeper", List.of(), 1, 10, GameInfo.MatchMethod.INSTANT);
            EmortalServer.registerGame(gameInfo, gameConfig);
        });
    }
}
