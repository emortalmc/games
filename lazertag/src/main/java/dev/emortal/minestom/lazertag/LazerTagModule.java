package dev.emortal.minestom.lazertag;

import dev.emortal.minestom.core.Module;
import dev.emortal.minestom.core.game.GameCreator;
import dev.emortal.minestom.core.map.MapManager;
import dev.emortal.minestom.core.map.MapManagerImpl;
import dev.emortal.minestom.lazertag.command.PingCompensationCommand;
import dev.emortal.minestom.lazertag.game.LazerTagGame;
import net.minestom.server.MinecraftServer;

import java.util.Set;

public class LazerTagModule implements Module {

    public LazerTagModule() {
        System.setProperty("minestom.tps", "60");
    }

    @Override
    public String getId() {
        return "lazertag";
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
        return LazerTagGame::new;
    }

    @Override
    public MapManager getMapManager() {
        return new MapManagerImpl(getId(), Set.of("dizzymc"));
    }

    @Override
    public void preRegister() {
        MinecraftServer.getCommandManager().register(new PingCompensationCommand());
    }
}
