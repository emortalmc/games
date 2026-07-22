package dev.emortal.minestom.lazertag.command;

import dev.emortal.minestom.core.utils.command.ExtraConditions;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import net.minestom.server.command.builder.condition.Conditions;

public class PingCompensationCommand extends Command {

    public static int TICKS = 8;

    public PingCompensationCommand() {
        super("pingcompensation");

        this.setCondition((sender, cmd) -> Conditions.playerOnly(sender, cmd) && ExtraConditions.hasPermission(sender, "command.lazertag.pingcompensation"));

        ArgumentInteger ticksArgument = ArgumentType.Integer("ticks");
        addSyntax((sender, ctx) -> {
            Integer ticks = ctx.get(ticksArgument);
            TICKS = ticks;
            sender.sendMessage("Set ping compensation to " + ticks);
        }, ticksArgument);
    }
}
