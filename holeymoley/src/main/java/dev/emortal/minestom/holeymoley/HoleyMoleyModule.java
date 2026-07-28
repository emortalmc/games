package dev.emortal.minestom.holeymoley;

import dev.emortal.minestom.core.Module;
import dev.emortal.minestom.core.game.GameCreator;
import dev.emortal.minestom.core.map.MapManager;
import dev.emortal.minestom.holeymoley.game.HoleyMoleyGame;
import dev.emortal.minestom.holeymoley.map.HoleyMoleyMapManager;
import io.github.togar2.pvp.MinestomPvP;

public class HoleyMoleyModule implements Module {
    public static final int MAP_RADIUS = 25;

    @Override
    public String getId() {
        return "holeymoley";
    }

    @Override
    public int getMinPlayers() {
        return 2;
    }

    @Override
    public int getMaxPlayers() {
        return 20;
    }

    @Override
    public GameCreator getGameCreator() {
        return HoleyMoleyGame::new;
    }

    @Override
    public MapManager getMapManager() {
        return new HoleyMoleyMapManager(MAP_RADIUS);
    }

    @Override
    public void preRegister() {
        MinestomPvP.init();
    }
}
