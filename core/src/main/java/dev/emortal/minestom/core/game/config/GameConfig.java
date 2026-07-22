package dev.emortal.minestom.core.game.config;

import dev.emortal.minestom.core.game.GameCreator;
import dev.emortal.minestom.core.map.MapManager;
import org.jetbrains.annotations.NotNull;

/**
 * The configuration that the game manager will use to create and manage games.
 *
 * @param gameCreator a function that can be called to create a game instance
 * @param mapManager
 */
public record GameConfig(FinishBehaviour finishBehaviour, @NotNull GameCreator gameCreator, MapManager mapManager) {
    public enum FinishBehaviour {
        LOBBY, REQUEUE
    }
}
