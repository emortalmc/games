package dev.emortal.minestom.blocksumo;

import dev.emortal.messaging.types.BlockSumoData;
import dev.emortal.minestom.blocksumo.command.GameCommand;
import dev.emortal.minestom.blocksumo.command.SaveLoadoutCommand;
import dev.emortal.minestom.blocksumo.game.BlockSumoGame;
import dev.emortal.minestom.core.EmortalServer;
import dev.emortal.minestom.core.Module;
import dev.emortal.minestom.core.game.GameCreator;
import dev.emortal.minestom.core.game.GameManager;
import dev.emortal.minestom.core.map.MapManager;
import dev.emortal.minestom.core.map.MapManagerImpl;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BlockSumoModule implements Module {
    public static final int MIN_PLAYERS = 2;
    public static final @NotNull BlockSumoData DEFAULT_PLAYER_DATA = new BlockSumoData(0, 45);

    @Override
    public String getId() {
        return "blocksumo";
    }

    @Override
    public int getMinPlayers() {
        return MIN_PLAYERS;
    }

    @Override
    public int getMaxPlayers() {
        return 20;
    }

    @Override
    public GameCreator getGameCreator() {
        return (info, map) -> {
            Map<UUID, BlockSumoData> playerData = new HashMap<>();
//            for (UUID uuid : info.playerIds()) {
//                if (db == null) continue;
//                playerData.put(uuid, db.getSettings(uuid));
//            }

            return new BlockSumoGame(info, map, playerData);
        };
    }

    @Override
    public MapManager getMapManager() {
        return new MapManagerImpl(getId(), Set.of(
                "blocksumo",
                "castle",
                "end",
                "ice",
                "ruins",
                "deepdark"
        ));
    }

    @Override
    public void postRegister() {
        GameManager gameManager = EmortalServer.getGameManager();
        MinecraftServer.getCommandManager().register(new GameCommand(gameManager));
        MinecraftServer.getCommandManager().register(new SaveLoadoutCommand(gameManager));
    }
}
