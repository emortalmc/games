package dev.emortal.minestom.lazertag.ping;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.packet.client.common.ClientPongPacket;
import net.minestom.server.network.packet.server.common.PingPacket;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerPinger {
    private int tick = 0;
    private final UUID playerUUID;
    private @Nullable Integer sentTick = null;
    private @Nullable Integer pingId = null; // null if responded
    private int ping = 0; // ping in ticks

    public PlayerPinger(Player player) {
        this.playerUUID = player.getUuid();

        player.scheduler().buildTask(() -> {
            int currentTick = ++tick;

            if (pingId != null) return;
            ThreadLocalRandom random = ThreadLocalRandom.current();
            pingId = random.nextInt();
            player.sendPacket(new PingPacket(pingId));
            sentTick = currentTick;
        }).repeat(TaskSchedule.tick(1)).schedule();

        MinecraftServer.getPacketListenerManager().setListener(ConnectionState.PLAY, ClientPongPacket.class, (packet, conn) -> {
            handlePong(packet, conn.getPlayer());
        });
    }

    private void handlePong(ClientPongPacket packet, Player player) {
        if (!player.getUuid().equals(playerUUID)) return;
        if (pingId == null || sentTick == null) return;
        if (packet.id() != pingId) return;

        ping = tick - sentTick;
        pingId = null;
    }

    public int getPing() {
        return ping;
    }

}
