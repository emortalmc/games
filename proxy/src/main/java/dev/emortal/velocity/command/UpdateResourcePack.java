package dev.emortal.velocity.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.velocity.resourcepack.ResourcePackSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class UpdateResourcePack {

    public static void registerCommand(Object plugin, CommandManager commandManager, ResourcePackSender rpSender) {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("updateresourcepack")
                .requires(c -> c.hasPermission("proxy.updateresourcepack"))
                .executes(ctx -> {
                    ctx.getSource().sendMessage(Component.text("Updating resourcepack sha1...", NamedTextColor.GRAY));
                    rpSender.updateResourcePackInfo();
                    ctx.getSource().sendMessage(Component.text("Updated!", NamedTextColor.GREEN));
                    return Command.SINGLE_SUCCESS;
                }).build();

        BrigadierCommand command = new BrigadierCommand(node);
        CommandMeta meta = commandManager.metaBuilder(command)
                .plugin(plugin)
                .build();
        commandManager.register(meta, command);
    }

}
