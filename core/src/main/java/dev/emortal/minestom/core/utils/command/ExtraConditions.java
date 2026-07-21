package dev.emortal.minestom.core.utils.command;

import dev.emortal.minestom.core.EmortalServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ExtraConditions {

    public static @NotNull CommandCondition hasPermission(@NotNull String permission) {
        return (sender, commandName) -> hasPermission(sender, permission);
    }

    public static boolean hasPermission(@NotNull CommandSender sender, @NotNull String permission) {
        if (sender instanceof ConsoleSender) return true;
        if (!(sender instanceof Player player)) return false;
        return EmortalServer.hasPermission(player, permission);
    }

    private ExtraConditions() {
        throw new AssertionError("This class cannot be instantiated.");
    }
}
