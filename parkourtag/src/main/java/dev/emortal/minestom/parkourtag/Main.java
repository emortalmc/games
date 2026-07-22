package dev.emortal.minestom.parkourtag;

import dev.emortal.minestom.core.EmortalServer;

public final class Main {
    void main() {
        EmortalServer.start(new ParkourTagModule());
    }
}