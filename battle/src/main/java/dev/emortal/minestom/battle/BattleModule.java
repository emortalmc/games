package dev.emortal.minestom.battle;

import dev.emortal.minestom.battle.game.BattleGame;
import dev.emortal.minestom.core.Module;
import dev.emortal.minestom.core.game.GameCreator;
import dev.emortal.minestom.core.map.MapManager;
import dev.emortal.minestom.core.map.MapManagerImpl;
import io.github.togar2.pvp.MinestomPvP;

import java.util.Set;

public class BattleModule implements Module {

    @Override
    public String getId() {
        return "battle";
    }

    @Override
    public int getMinPlayers() {
        return 2;
    }

    @Override
    public int getMaxPlayers() {
        return 16;
    }

    @Override
    public GameCreator getGameCreator() {
        return BattleGame::new;
    }

    @Override
    public MapManager getMapManager() {
        return new MapManagerImpl(getId(), Set.of(
                "caverns",
                "cove",
                "crucible"
        ));
    }

    @Override
    public void preRegister() {
        MinestomPvP.init();
    }
}
