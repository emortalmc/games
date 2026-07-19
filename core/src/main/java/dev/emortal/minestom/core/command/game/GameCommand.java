package dev.emortal.minestom.core.command.game;

import dev.emortal.minestom.core.game.Game;
import dev.emortal.minestom.core.game.GameManager;
import dev.emortal.minestom.core.game.config.GameCreationInfo;
import dev.emortal.minestom.core.utils.command.ExtraConditions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentLiteral;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class GameCommand extends Command {

    private final @NotNull GameManager gameManager;

    public GameCommand(@NotNull GameManager gameManager) {
        super("game");
        this.gameManager = gameManager;

        this.setCondition(ExtraConditions.hasPermission("command.game"));

        this.addConditionalSyntax(ExtraConditions.hasPermission("command.game.start"), this::executeStart, new ArgumentLiteral("start"));
        this.addConditionalSyntax(ExtraConditions.hasPermission("command.game.list"), this::executeList, new ArgumentLiteral("list"));
    }

    private void executeStart(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("You must be a player to use this command as it targets the Game you are in.");
            return;
        }

        Game game = this.gameManager.findGame(player);
        if (game == null) {
            sender.sendMessage("You must be in a game to use this command!");
            return;
        }

        sender.sendMessage("Starting game...");
        this.gameManager.startGame(game);
    }

    private void executeList(@NotNull CommandSender sender, @NotNull CommandContext context) {
        TextComponent.Builder messageBuilder = Component.text().append(Component.text("Games: ", NamedTextColor.LIGHT_PURPLE));

        for (Game game : this.gameManager.getGames()) {
            GameCreationInfo creationInfo = game.getCreationInfo();

            messageBuilder.append(Component.newline());
            messageBuilder.append(Component.text("  %s (%s players): ".formatted(creationInfo.gameId(), game.getPlayers().size()), NamedTextColor.LIGHT_PURPLE));

            List<Component> onlineComponents = new ArrayList<>();
            List<Component> offlineComponents = new ArrayList<>();

            // Loop through ALL initial players, not just currently connected
            for (UUID playerId : creationInfo.playerIds()) {
                Player connectedPlayer = null;
                for (Player player : game.getPlayers()) {
                    if (player.getUuid().equals(playerId)) {
                        connectedPlayer = player;
                        break;
                    }
                }

                if (connectedPlayer != null) {
                    HoverEvent<Component> hoverEvent = HoverEvent.showText(Component.text(playerId.toString(), NamedTextColor.GREEN));
                    onlineComponents.add(Component.text(connectedPlayer.getUsername(), NamedTextColor.GREEN).hoverEvent(hoverEvent));
                } else {
                    offlineComponents.add(Component.text(playerId.toString(), NamedTextColor.GRAY));
                }
            }

            List<Component> allPlayerComponents = new ArrayList<>();
            allPlayerComponents.addAll(onlineComponents);
            allPlayerComponents.addAll(offlineComponents);

            Component playerListComponent = Component.join(JoinConfiguration.commas(true), allPlayerComponents);

            messageBuilder.append(playerListComponent);
        }

        sender.sendMessage(messageBuilder.build());
    }
}
