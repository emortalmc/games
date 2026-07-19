package dev.emortal.minestom.core.game.config;

import dev.emortal.minestom.core.game.GameCreator;
import org.jetbrains.annotations.NotNull;

/**
 * The configuration that the game manager will use to create and manage games.
 *
 * @param minPlayers  the minimum players required for a game to start
 * @param gameCreator a function that can be called to create a game instance
 */
public record GameConfig(int minPlayers, FinishBehaviour finishBehaviour, @NotNull GameCreator gameCreator) {
    public enum FinishBehaviour {
        LOBBY, REQUEUE
    }
}
