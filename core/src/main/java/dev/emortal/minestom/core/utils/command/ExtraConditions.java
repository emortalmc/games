package dev.emortal.minestom.core.utils.command;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.command.builder.condition.CommandCondition;
import org.jetbrains.annotations.NotNull;

public final class ExtraConditions {

    public static @NotNull CommandCondition hasPermission(@NotNull String permission) {
        return (sender, commandName) -> hasPermission(sender, permission);
    }

    public static boolean hasPermission(@NotNull CommandSender sender, @NotNull String permission) {
        if (sender instanceof ConsoleSender) return true;
        // TODO: permission system
        return false;
    }

    private ExtraConditions() {
        throw new AssertionError("This class cannot be instantiated.");
    }
}
