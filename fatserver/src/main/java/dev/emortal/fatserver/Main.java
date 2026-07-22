package dev.emortal.fatserver;

import dev.emortal.minestom.battle.BattleModule;
import dev.emortal.minestom.blocksumo.BlockSumoModule;
import dev.emortal.minestom.core.EmortalServer;
import dev.emortal.minestom.marathon.MarathonModule;
import dev.emortal.minestom.minesweeper.MinesweeperModule;
import dev.emortal.minestom.parkourtag.ParkourTagModule;

public class Main {
    void main() {
        EmortalServer.start(
                new MarathonModule(),
                new ParkourTagModule(),
                new BattleModule(),
                new BlockSumoModule(),
//                new LazerTagModule(), Has 60 TPS so cannot be used in fat server
                new MinesweeperModule()
        );
    }
}
