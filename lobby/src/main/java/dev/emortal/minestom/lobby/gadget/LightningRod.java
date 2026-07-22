package dev.emortal.minestom.lobby.gadget;

import dev.emortal.minestom.lobby.util.SphereUtil;
import dev.emortal.minestom.lobby.util.WorldBlock;
import dev.emortal.minestom.lobby.util.entity.BetterEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ExplosionPacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.WeightedList;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public final class LightningRod extends BlockChangingGadget {
    private static final Set<Point> BLOCKS_IN_SPHERE = SphereUtil.getBlocksInSphere(2.0);
    private static final ItemStack ITEM = ItemStack.builder(Material.LIGHTNING_ROD)
            .set(DataComponents.CUSTOM_NAME, Component.text("Lightning Rod").decoration(TextDecoration.ITALIC, false))
            .build();

    public LightningRod() {
        super("Lightning Rod", ITEM, "lightning_rod", false);
    }

    @Override
    protected void onUse(@NotNull Player user, @NotNull Instance instance) {
        Point hitPos = user.getTargetBlockPosition(500);
        if (hitPos == null) return;

        BetterEntity bolt = new BetterEntity(EntityType.LIGHTNING_BOLT);
        bolt.setNoGravity(true);
        bolt.setPhysics(false);

        bolt.setInstance(instance, hitPos);
        bolt.scheduleRemove(3 * 20, TimeUnit.SERVER_TICK);

        List<WorldBlock> blocks = SphereUtil.getNearbyBlocks(hitPos, BLOCKS_IN_SPHERE, instance, block -> !block.air());

        this.explodeBlocks(instance, blocks);
        user.sendPacket(new ExplosionPacket(hitPos, 2f, 0, null, Particle.EXPLOSION_EMITTER, SoundEvent.ENTITY_GENERIC_EXPLODE, WeightedList.of()));

        super.regenerateInstanceBlocks(instance, blocks);
    }

    private void explodeBlocks(@NotNull Instance instance, @NotNull List<WorldBlock> blocks) {
        AbsoluteBlockBatch batch = new AbsoluteBlockBatch();
        for (WorldBlock block : blocks) {
            batch.setBlock(block.position(), Block.AIR);
        }
        batch.apply(instance, null);
    }
}
