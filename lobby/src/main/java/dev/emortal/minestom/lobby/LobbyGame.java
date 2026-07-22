package dev.emortal.minestom.lobby;

import dev.emortal.minestom.core.game.Game;
import dev.emortal.minestom.core.game.config.GameCreationInfo;
import dev.emortal.minestom.core.map.LoadedMap;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public class LobbyGame extends Game {
    public LobbyGame(GameCreationInfo info, LoadedMap map) {
        super(info, map);
    }

    @Override
    public void start() {

    }

    @Override
    public void cleanUp() {

    }

    @Override
    public void onPreJoin(@NotNull Player player) {

    }

    @Override
    public void onJoin(@NotNull Player player) {

    }

    @Override
    public void onLeave(@NotNull Player player) {

    }

    @Override
    public @NotNull Instance getSpawningInstance(@NotNull Player player) {
        return getMap().instance();
    }
}
