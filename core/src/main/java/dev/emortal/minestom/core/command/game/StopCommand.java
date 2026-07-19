package dev.emortal.minestom.core.command.game;

import dev.emortal.minestom.core.EmortalServer;
import dev.emortal.minestom.core.utils.command.ExtraConditions;
import net.minestom.server.command.builder.Command;

public final class StopCommand extends Command {
    public StopCommand() {
        super("stop");

        this.setCondition(ExtraConditions.hasPermission("command.stop"));
        this.setDefaultExecutor((sender, ctx) -> {
            EmortalServer.stop();
        });
    }
}
