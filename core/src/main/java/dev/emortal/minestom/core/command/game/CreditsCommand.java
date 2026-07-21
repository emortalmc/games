package dev.emortal.minestom.core.command.game;

import dev.emortal.minestom.core.game.Game;
import dev.emortal.minestom.core.game.GameProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CreditsCommand extends Command {
    private static final @NotNull Component UNKNOWN_MESSAGE = Component.text("We're not sure who made this map", NamedTextColor.RED);

    private final @NotNull GameProvider gameProvider;

    public CreditsCommand(@NotNull GameProvider gameProvider) {
        super("credits");
        this.gameProvider = gameProvider;

        super.setCondition(Conditions::playerOnly);
        super.setDefaultExecutor(this::execute);
    }

    private void execute(@NotNull CommandSender sender, @NotNull CommandContext context) {
        Game game = this.gameProvider.findGame((Player) sender);
        if (game == null) return;
        if (game.getMap() == null) return;

        List<String> mapUsernames = game.getMap().data().getList("credits", String.class);
        if (mapUsernames == null || mapUsernames.isEmpty()) {
            sender.sendMessage(UNKNOWN_MESSAGE);
            return;
        }

        TextComponent.Builder message = Component.text();
        message.append(Component.text("You're currently playing on ", NamedTextColor.LIGHT_PURPLE));
        message.append(Component.text(game.getMap().data().getString("name"), NamedTextColor.LIGHT_PURPLE));
        message.append(Component.text(", created by:", NamedTextColor.LIGHT_PURPLE));

        for (String mapUsername : mapUsernames) {
            message.append(Component.newline());
            message.append(Component.text(" - "));
            message.append(Component.text(mapUsername));
        }

        sender.sendMessage(message.build());
    }
}
