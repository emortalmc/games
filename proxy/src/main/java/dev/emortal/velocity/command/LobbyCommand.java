package dev.emortal.velocity.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.velocity.CorePlugin;

public class LobbyCommand {

    public static void registerCommand(CorePlugin plugin, CommandManager commandManager) {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("lobby")
                .executes(ctx -> {
                    if (!(ctx.getSource() instanceof Player player)) return Command.SINGLE_SUCCESS;

                    plugin.sendToLobby(player);

                    return Command.SINGLE_SUCCESS;
                }).build();

        BrigadierCommand command = new BrigadierCommand(node);
        CommandMeta meta = commandManager.metaBuilder(command)
                .aliases("l", "hub")
                .plugin(plugin)
                .build();
        commandManager.register(meta, command);
    }

}
