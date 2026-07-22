package dev.emortal.minestom.minesweeper;

import dev.emortal.messaging.types.GameInfo;
import dev.emortal.minestom.core.Module;
import dev.emortal.minestom.core.game.GameCreator;
import dev.emortal.minestom.core.game.config.GameConfig;
import dev.emortal.minestom.core.map.MapManager;
import dev.emortal.minestom.minesweeper.board.Board;
import dev.emortal.minestom.minesweeper.game.MinesweeperGame;
import dev.emortal.minestom.minesweeper.map.MinesweeperMapManager;

public class MinesweeperModule implements Module {
    @Override
    public String getId() {
        return "minesweeper";
    }

    @Override
    public int getMinPlayers() {
        return 1;
    }

    @Override
    public int getMaxPlayers() {
        return 5;
    }

    @Override
    public GameCreator getGameCreator() {
        return (info, map) -> {
            Board board = ((MinesweeperMapManager) getMapManager()).getOrCreateMap(info.playerIds());
            return new MinesweeperGame(info, board);
        };
    }

    @Override
    public MapManager getMapManager() {
        return new MinesweeperMapManager();
    }

    @Override
    public GameConfig.FinishBehaviour getFinishBehaviour() {
        return GameConfig.FinishBehaviour.REQUEUE;
    }

    @Override
    public GameInfo.MatchMethod getMatchMethod() {
        return GameInfo.MatchMethod.INSTANT;
    }
}
