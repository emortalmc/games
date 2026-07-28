package dev.emortal.minestom.holeymoley.event;

import dev.emortal.minestom.holeymoley.game.HoleyMoleyGame;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;

public class TrapdoorEvent extends Event {

    public TrapdoorEvent() {
        super("Trapdoor");
    }

    @Override
    public void doEvent(HoleyMoleyGame game) {
        for (Player player : game.getAlivePlayers()) {
            for (int i = 0; i < 20; i++) {
                Pos pos = player.getPosition().sub(0, i, 0);
                if (game.getInstance().getBlock(pos, Block.Getter.Condition.TYPE).compare(Block.BEDROCK)) break;
                game.getInstance().setBlock(pos, Block.AIR);
            }
        }
    }
}
