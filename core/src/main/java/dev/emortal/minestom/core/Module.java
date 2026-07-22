package dev.emortal.minestom.core;

import dev.emortal.messaging.types.GameInfo;
import dev.emortal.minestom.core.game.GameCreator;
import dev.emortal.minestom.core.game.config.GameConfig;
import dev.emortal.minestom.core.map.MapManager;

public interface Module {

    String getId();

    int getMinPlayers();

    int getMaxPlayers();

    default GameInfo.MatchMethod getMatchMethod() {
        return GameInfo.MatchMethod.COUNTDOWN;
    }

    default GameConfig.FinishBehaviour getFinishBehaviour() {
        return GameConfig.FinishBehaviour.LOBBY;
    }

    GameCreator getGameCreator();

    MapManager getMapManager();

    default void preRegister() {}
    default void postRegister() {}

    default void register() {
        preRegister();
        MapManager mapManager = getMapManager();
        EmortalServer.registerGame(
                new GameInfo(getId(), mapManager.getMaps(), getMinPlayers(), getMaxPlayers(), getMatchMethod()),
                new GameConfig(getFinishBehaviour(), getGameCreator(), mapManager)
        );
        postRegister();
    }

}
