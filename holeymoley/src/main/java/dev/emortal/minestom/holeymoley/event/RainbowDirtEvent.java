package dev.emortal.minestom.holeymoley.event;

import dev.emortal.minestom.holeymoley.game.HoleyMoleyGame;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RainbowDirtEvent extends Event {

    private static final Block[] RAINBOW_BLOCKS = new Block[]{
            Block.RED_CONCRETE_POWDER,
            Block.ORANGE_CONCRETE_POWDER,
            Block.YELLOW_CONCRETE_POWDER,
            Block.GREEN_CONCRETE_POWDER,
            Block.BLUE_CONCRETE_POWDER,
            Block.CYAN_CONCRETE_POWDER,
            Block.LIGHT_BLUE_CONCRETE_POWDER,
            Block.PURPLE_CONCRETE_POWDER,
            Block.PINK_CONCRETE_POWDER,
            Block.MAGENTA_CONCRETE_POWDER
    };

    public RainbowDirtEvent() {
        super("Rainbow Dirt");
    }

    @Override
    public void doEvent(HoleyMoleyGame game) {
        List<Point> sphereBlocks = getBlocksInSphere(4);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (Player player : game.getAlivePlayers()) {
            for (Point sphereBlock : sphereBlocks) {
                Pos pos = player.getPosition().add(sphereBlock);

                if (!game.getInstance().getBlock(pos, Block.Getter.Condition.TYPE).compare(Block.DIRT)) continue;
                game.getInstance().setBlock(pos, RAINBOW_BLOCKS[random.nextInt(RAINBOW_BLOCKS.length)]);
            }
        }
    }

    private List<Point> getBlocksInSphere(int radius) {
        List<Point> points = new ArrayList<>();

        for (int x = -radius; x < radius; x++) {
            for (int y = -radius; y < radius; y++) {
                for (int z = -radius; z < radius; z++) {
                    if (x * x + y * y + z * z > radius * radius) continue;

                    points.add(new Vec(x, y, z));
                }
            }
        }

        return points;
    }
}
