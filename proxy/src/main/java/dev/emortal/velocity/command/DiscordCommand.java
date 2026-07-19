package dev.emortal.velocity.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class DiscordCommand {

    private static final Component DISCORD_MESSAGE = MiniMessage.miniMessage().deserialize("<click:open_url:'https://discord.com/invite/TZyuMSha96'><gradient:#7289da:#51629c:#51629c>Click to join our</gradient> <#7289da><bold>Discord</bold><#51629c>!</click>");

    public static void registerCommand(Object plugin, CommandManager commandManager) {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("discord")
                .executes(ctx -> {
                    ctx.getSource().sendMessage(DISCORD_MESSAGE);
                    return Command.SINGLE_SUCCESS;
                }).build();

        BrigadierCommand command = new BrigadierCommand(node);
        CommandMeta meta = commandManager.metaBuilder(command)
                .plugin(plugin)
                .build();
        commandManager.register(meta, command);
    }

}
