package dev.emortal.minestom.marathon;

import dev.emortal.minestom.core.EmortalServer;

public final class Main {
    void main() {
        EmortalServer.start(new MarathonModule());
    }
}
