package dev.emortal.minestom.lobby;

import dev.emortal.minestom.core.EmortalServer;

public final class Main {
    void main() {
//        PlayerPack.writePlayerPack(new File("epicpack"));
        EmortalServer.start(new LobbyModule());

//        MinecraftServer.getSchedulerManager().buildTask(() -> {
//            EmortalServer.stop();
//        }).delay(TaskSchedule.tick(10 * 20)).schedule();
    }
}
