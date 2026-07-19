package dev.emortal.minestom.core.game.util;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class GameWinLoseMessages {

    private static final List<String> VICTORY = List.of(
            "pepeD",
            "gg ez no re",
            "All luck",
            "NoHacksJustSkill",
            "Were you hacking?",
            "Were you teaming?",
            "no skill issues here",
            "i am better",
            "I dropped my popcorn",
            "Defeat... but not for you",
            "Teamwork makes the dream work",
            "vic roy",
            "call an ambulance... but not for me",
            "What chair do you have?",
            "I won? It must have been my chair!",
            "No GC this time",
            "Moulberry is proud of you",
            "tryhard == true",
            "touch grass",
            "take a shower",
            "take a cookie for your efforts",
            "[username] never dies!",
            "average rust enjoyer",
            "average valence enjoyer",
            "average minestom enjoyer",
            "i wasn't looking, do it again",
            "built different",
            "carried",
            "don't forget to breathe!",
            "want a medal?",
            "you lost the game",
            "BLEHHH"
    );
    private static final List<String> DEFEAT = List.of(
            "I guess you weren't trying",
            "no",
            "L",
            "rip bozo",
            "no zaza??",
            "what is your excuse?",
            "skill issue",
            "uffeyman was here",
            "Better luck next time!",
            "Worse luck next time!",
            "Should have used a Minestom client",
            "Teamwork... towards defeat",
            "No one matches your skill... at losing",
            "Victory... but not for you",
            "You tried and that's what matters!",
            "Zzzzzz",
            "Did your cat step on your keyboard?",
            "PepeHands",
            "At least someone won, too bad it wasn't you",
            "cry about it",
            "were you afk?",
            "smh my head",
            "I bet you didn't even say good game",
            "You did your best... but it wasn't enough",
            "\"i lagged\" said an Australian",
            "If I had a dollar every time you won a game, I'd be in crippling debt",
            "widepeepoSad",
            "try harder",
            "You clearly weren't using Graphite",
            "I saw the GC spike",
            "python user",
            "BLEHHH",
            "Kotlin user"
    );

    public static @NotNull String randomVictory() {
        return random(VICTORY);
    }

    public static @NotNull String randomDefeat() {
        return random(DEFEAT);
    }

    private static @NotNull String random(@NotNull List<String> messages) {
        int length = messages.size();
        int randomIndex = ThreadLocalRandom.current().nextInt(length);
        return messages.get(randomIndex);
    }

    private GameWinLoseMessages() {
    }
}
