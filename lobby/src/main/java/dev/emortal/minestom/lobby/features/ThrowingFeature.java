package dev.emortal.minestom.lobby.features;

import dev.emortal.minestom.core.utils.command.ExtraConditions;
import net.minestom.server.ServerFlag;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.player.PlayerInputEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public final class ThrowingFeature implements LobbyFeature {

    @Override
    public void register(@NotNull Instance instance) {
        instance.eventNode().addListener(PlayerEntityInteractEvent.class, event -> {
            if (!(event.getTarget() instanceof Player target)) return;
            Player player = event.getPlayer();

            if (!ExtraConditions.hasPermission(player, "lobby.throw_players")) return;

            if (player.isSneaking()) player.addPassenger(target);
        });

        instance.eventNode().addListener(PlayerInputEvent.class, event -> {
            if (!event.hasReleasedShiftKey()) return; // listen for player stop sneaking

            Player player = event.getPlayer();
            if (player.getPassengers().isEmpty()) return;
            if (!ExtraConditions.hasPermission(player, "lobby.throw_players")) return;

            for (Entity passenger : player.getPassengers()) {
                if (!(passenger instanceof Player)) continue;
                player.removePassenger(passenger);
                passenger.setVelocity(player.getPosition().direction().mul(ServerFlag.SERVER_TICKS_PER_SECOND).mul(4));
            }
        });
    }
}
