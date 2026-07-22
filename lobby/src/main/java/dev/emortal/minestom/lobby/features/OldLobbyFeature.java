package dev.emortal.minestom.lobby.features;

import dev.emortal.minestom.core.utils.PolarUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class OldLobbyFeature implements LobbyFeature {

    private static final Point OLD_LOBBY_SIGN_POS = new Vec(-10, 66, 10);
    private static final Point OLD_LOBBY_SPAWN_POS = new Vec(0.5, 100, 0.5);

    @Override
    public void register(@NotNull Instance instance) {
        InstanceContainer oldInstance = MinecraftServer.getInstanceManager().createInstanceContainer();
        oldInstance.enableAutoChunkLoad(false);
        PolarUtil.stream(oldInstance, Path.of("oldlobby.polar")).join();

        instance.eventNode().addListener(PlayerBlockInteractEvent.class, event -> {
            if (!event.getBlockPosition().sameBlock(OLD_LOBBY_SIGN_POS)) return;

            event.getPlayer().setInstance(oldInstance, OLD_LOBBY_SPAWN_POS);
            event.getPlayer().sendMessage(Component.text("Welcome to the old EmortalMC lobby! Use /spawn to get back to the new lobby.", NamedTextColor.AQUA));
        });
    }
}
