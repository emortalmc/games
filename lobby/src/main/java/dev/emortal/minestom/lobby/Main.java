package dev.emortal.minestom.lobby;

import dev.emortal.minestom.core.EmortalServer;

public final class Main {
    void main() {
        EmortalServer.start(new LobbyModule());
    }
}
