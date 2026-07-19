package dev.emortal.minestom.core.game;

import net.minestom.server.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * This is called by a game when it is finished to signal to the game manager that
 * it should be removed, and that all the players on it should be kicked.
 *
 * <p>
 * The game manager will then tell the game to clean itself up, and then the game
 * should no longer exist after that.
 *
 * <p>
 * The reason why we do it like this is that I didn't like the idea of the games
 * having any knowledge of how they are managed, or that they are managed at all,
 * and I wanted to have the relationship be one way, meaning that the game manager knows
 * about the games that exist, but the games don't know the game manager exists.
 */
public record GameFinishedEvent(@NotNull Game game) implements Event {
}
