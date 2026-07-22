package dev.emortal.minestom.battle;

import dev.emortal.minestom.core.EmortalServer;

public final class Main {
    void main() {
        EmortalServer.start(new BattleModule());
    }
}