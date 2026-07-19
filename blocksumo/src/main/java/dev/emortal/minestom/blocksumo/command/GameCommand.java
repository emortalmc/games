package dev.emortal.minestom.blocksumo.command;

import dev.emortal.minestom.blocksumo.event.EventManager;
import dev.emortal.minestom.blocksumo.event.events.BlockSumoEvent;
import dev.emortal.minestom.blocksumo.game.BlockSumoGame;
import dev.emortal.minestom.blocksumo.powerup.PowerUp;
import dev.emortal.minestom.blocksumo.powerup.PowerUpManager;
import dev.emortal.minestom.core.game.Game;
import dev.emortal.minestom.core.game.GameProvider;
import dev.emortal.minestom.core.utils.command.ExtraConditions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentLiteral;
import net.minestom.server.command.builder.arguments.ArgumentWord;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GameCommand extends Command {

    private final @NotNull GameProvider gameProvider;

    public GameCommand(@NotNull GameProvider gameProvider) {
        super("game");
        this.gameProvider = gameProvider;

        this.setCondition((sender, cmd) -> Conditions.playerOnly(sender, cmd) && ExtraConditions.hasPermission(sender, "command.game.blocksumo"));

        ArgumentLiteral event = new ArgumentLiteral("event");
        ArgumentLiteral start = new ArgumentLiteral("start");
        ArgumentWord eventType = new ArgumentWord("eventType");
        Argument<Integer> count = new ArgumentInteger("count").min(1).setDefaultValue(1);
        Argument<Integer> delay = new ArgumentInteger("delay").min(1).setDefaultValue(1);
        eventType.setSuggestionCallback((sender, context, suggestion) -> this.suggestEvents(sender, suggestion));

        // /game event start [count] [delay]
        this.addSyntax(this::executeStartEvent, event, start, count, delay);
        // /game event start <eventType> [count] [delay]
        this.addSyntax(this::executeStartEvent, event, start, eventType, count, delay);

        ArgumentLiteral powerup = new ArgumentLiteral("powerup");
        ArgumentLiteral give = new ArgumentLiteral("give");
        ArgumentLiteral giveAll = new ArgumentLiteral("giveall");
        ArgumentLiteral spawn = new ArgumentLiteral("spawn");
        ArgumentWord powerUpType = new ArgumentWord("powerUpType");
        powerUpType.setSuggestionCallback((sender, context, suggestion) -> this.suggestPowerUps(sender, suggestion));

        // /game powerup give [powerUpType]
        this.addSyntax(this::executeGivePowerUp, powerup, give);
        this.addSyntax(this::executeGivePowerUp, powerup, give, powerUpType);
        // /game powerup giveall [powerUpType]
        this.addSyntax(this::executeGiveAllPowerUp, powerup, giveAll);
        this.addSyntax(this::executeGiveAllPowerUp, powerup, giveAll, powerUpType);
        // /game powerup spawn [powerUpType]
        this.addSyntax(this::executeSpawnPowerUp, powerup, spawn);
        this.addSyntax(this::executeSpawnPowerUp, powerup, spawn, powerUpType);
    }

    private @Nullable BlockSumoGame getGame(@NotNull CommandSender sender) {
        Game game = this.gameProvider.findGame((Player) sender);
        if (game == null) {
            sender.sendMessage("You are not in a game!");
            return null;
        }

        return (BlockSumoGame) game;
    }

    private void suggestEvents(@NotNull CommandSender sender, @NotNull Suggestion suggestion) {
        BlockSumoGame game = this.getGame(sender);
        if (game == null) return;

        for (String eventName : game.getEventManager().getEventNames()) {
            suggestion.addEntry(new SuggestionEntry(eventName));
        }
    }

    private void executeStartEvent(@NotNull CommandSender sender, @NotNull CommandContext context) {
        BlockSumoGame game = this.getGame(sender);
        if (game == null) return;

        String eventName = context.has("eventType") ? context.get("eventType") : null;
        int count = context.get("count");
        int delay = context.get("delay");
        EventManager eventManager = game.getEventManager();

        for (int i = 1; i <= count; i++) {
            MinecraftServer.getSchedulerManager().buildTask(() -> {
                BlockSumoEvent event;
                if (eventName != null) {
                    event = eventManager.findNamedEvent(eventName);
                    if (event == null) {
                        sender.sendMessage(Component.text("Could not find event with name " + eventName + "!", NamedTextColor.RED));
                        return;
                    }
                } else {
                    event = eventManager.findRandomEvent();
                }

                eventManager.startEvent(event);
                sender.sendMessage(Component.text("Started event " + event.getClass().getSimpleName() + "!", NamedTextColor.GREEN));
                // TODO: Set current event in game
            }).delay(TaskSchedule.tick(i * delay)).schedule();
        }
    }

    private void suggestPowerUps(@NotNull CommandSender sender, @NotNull Suggestion suggestion) {
        BlockSumoGame game = this.getGame(sender);
        if (game == null) return;

        for (String powerUpName : game.getPowerUpManager().getPowerUpIds()) {
            suggestion.addEntry(new SuggestionEntry(powerUpName));
        }
    }

    private void executeGivePowerUp(@NotNull CommandSender sender, @NotNull CommandContext context) {
        this.giveOrSpawnPowerUp(sender, context, PowerUpAction.GIVE);
    }

    private void executeGiveAllPowerUp(@NotNull CommandSender sender, @NotNull CommandContext context) {
        this.giveOrSpawnPowerUp(sender, context, PowerUpAction.GIVE_ALL);
    }

    private void executeSpawnPowerUp(@NotNull CommandSender sender, @NotNull CommandContext context) {
        this.giveOrSpawnPowerUp(sender, context, PowerUpAction.SPAWN);
    }

    private void giveOrSpawnPowerUp(@NotNull CommandSender sender, @NotNull CommandContext context, @NotNull PowerUpAction action) {
        BlockSumoGame game = this.getGame(sender);
        if (game == null) return;

        String powerUpName = context.has("powerUpType") ? context.get("powerUpType") : null;
        PowerUpManager powerUpManager = game.getPowerUpManager();

        PowerUp powerUp;
        if (powerUpName != null) {
            powerUp = powerUpManager.findNamedPowerUp(powerUpName);
            if (powerUp == null) {
                sender.sendMessage(Component.text("Could not find power up with name " + powerUpName + "!", NamedTextColor.RED));
                return;
            }
        } else {
            powerUp = powerUpManager.findRandomPowerUp();
        }

        switch (action) {
            case GIVE -> powerUpManager.givePowerUp((Player) sender, powerUp);
            case GIVE_ALL -> powerUpManager.givePowerUpToAll(powerUp);
            case SPAWN -> powerUpManager.spawnPowerUp(powerUp);
        }
    }

    private enum PowerUpAction {

        GIVE,
        GIVE_ALL,
        SPAWN
    }
}
