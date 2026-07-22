package dev.emortal.minestom.minesweeper;

import dev.emortal.minestom.core.EmortalServer;

public final class Main {
    void main() {
        EmortalServer.start(new MinesweeperModule());
    }
}
