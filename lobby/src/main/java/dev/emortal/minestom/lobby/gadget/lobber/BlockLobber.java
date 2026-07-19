package dev.emortal.minestom.lobby.gadget.lobber;

import dev.emortal.minestom.core.raycast.RaycastUtil;
import dev.emortal.minestom.lobby.features.SeatingFeature;
import dev.emortal.minestom.lobby.gadget.Gadget;
import dev.emortal.minestom.lobby.util.SphereUtil;
import dev.emortal.minestom.lobby.util.WorldBlock;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.ServerFlag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityProjectile;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.FallingBlockMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.WorldEventPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class BlockLobber extends Gadget {
    private static final Set<Point> BLOCKS_IN_SPHERE = SphereUtil.getBlocksInSphere(3.0);
    private static final ItemStack ITEM = ItemStack.builder(Material.GOLDEN_SHOVEL)
            .set(DataComponents.CUSTOM_NAME, Component.text("Block Lobber").decoration(TextDecoration.ITALIC, false))
            .build();

    private static final double GRAVITY = -0.04;

    public BlockLobber() {
        super("Block Lobber", ITEM, "block_lobber", true);
    }

    @Override
    protected void onUse(@NotNull Player user, @NotNull Instance instance) {
        Pos pos = user.getPosition();

        Point randomNearbyBlock = this.getRandomNearbyBlock(pos, instance);
        if (randomNearbyBlock == null) {
            this.notifyTooFarAway(user);
            return;
        }

        this.playLobSound(instance, pos);
        Block randomBlock = instance.getBlock(randomNearbyBlock, Block.Getter.Condition.TYPE);

        EntityProjectile thrownBlockEntity = new BlockLobberEntity(user, randomBlock);

        Vec lookDirection = pos.direction();
        Player target = this.getRandomTarget(instance, user, lookDirection);

        Point initialTargetPosition;
        if (target == null) {
            initialTargetPosition = pos.add(lookDirection.mul(10));
            thrownBlockEntity.setVelocity(initialTargetPosition.sub(randomNearbyBlock).asVec().normalize().mul(35));
        } else {
            double displacementY = target.getPosition().y() - randomNearbyBlock.y();
            Point displacementXZ = target.getPosition().sub(randomNearbyBlock);
            double height = Math.max(displacementY, 0);

            double time = Math.sqrt(-2 * height / GRAVITY) + Math.sqrt(2 * (displacementY - height) / GRAVITY);
            double velocityY = Math.sqrt(-2 * GRAVITY * height);
            Point velocityXZ = displacementXZ.div(time);

            Vec combinedVelocity = velocityXZ.withY(velocityY).asVec();
            thrownBlockEntity.setVelocity(combinedVelocity.mul(ServerFlag.SERVER_TICKS_PER_SECOND));

            initialTargetPosition = target.getPosition();
        }

        instance.setBlock(randomNearbyBlock, Block.AIR);
        instance.scheduler().buildTask(() -> instance.setBlock(randomNearbyBlock, randomBlock)).delay(TaskSchedule.seconds(5)).schedule();

        Point spawnPoint = this.findValidSpawnPoint(instance, randomNearbyBlock, initialTargetPosition);
        thrownBlockEntity.setInstance(instance, spawnPoint).thenRun(() -> {
            // if the block thrown is a seat, throw the player too
            for (Map.Entry<Point, Entity> pointEntityEntry : SeatingFeature.armorStandSeats.entrySet()) {
                if (!pointEntityEntry.getKey().sameBlock(randomNearbyBlock)) continue;

                Entity seatEntity = SeatingFeature.armorStandSeats.remove(pointEntityEntry.getKey());
                if (!seatEntity.getPassengers().iterator().hasNext()) return;

                Player passenger = (Player) seatEntity.getPassengers().iterator().next();
                seatEntity.remove();

                thrownBlockEntity.addPassenger(passenger);
                break;
            }
        });
    }

    private @Nullable Point getRandomNearbyBlock(@NotNull Point pos, @NotNull Instance instance) {
        List<WorldBlock> blocks = SphereUtil.getNearbyBlocks(pos, BLOCKS_IN_SPHERE, instance, block -> !block.isAir());
        if (blocks.isEmpty()) return null;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        WorldBlock randomBlock = blocks.get(random.nextInt(blocks.size()));
        return randomBlock.position();
    }

    private void notifyTooFarAway(@NotNull Player player) {
        player.playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_NO, Sound.Source.MASTER, 1F, 1F), Sound.Emitter.self());
        player.sendActionBar(Component.text("Get closer to some blocks to lob them!", NamedTextColor.RED));
    }

    private void playLobSound(@NotNull Instance instance, @NotNull Point pos) {
        instance.playSound(Sound.sound(SoundEvent.ENTITY_GHAST_SHOOT, Sound.Source.MASTER, 0.6F, 2F), pos);
    }

    private @Nullable Player getRandomTarget(@NotNull Instance instance, @NotNull Player player, @NotNull Vec lookDirection) {
        List<Player> availableTargets = new ArrayList<>();

        for (Player other : instance.getPlayers()) {
            if (other == player) continue;

            Vec directionToPlayer = other.getPosition().sub(player.getPosition()).asVec().normalize();
            if (!RaycastUtil.hasLineOfSight(player, other)) continue;

            double similarity = cosineSimilarity(directionToPlayer, lookDirection);
            if (similarity < 0.99) continue;

            availableTargets.add(other);
        }
        if (availableTargets.isEmpty()) return null;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        return availableTargets.get(random.nextInt(availableTargets.size()));
    }

    private @NotNull Point findValidSpawnPoint(@NotNull Instance instance, @NotNull Point pos, @NotNull Point initialTargetPosition) {
        Point direction = initialTargetPosition.sub(pos).asVec().normalize();
        Point newPos = pos;

        int tries = 0;
        while (tries < 3) {
            tries++;

            newPos = newPos.add(direction);
            if (RaycastUtil.hasLineOfSight(instance, newPos, initialTargetPosition)) {
                // escaped!
                return newPos.add(direction.mul(0.7));
            }
        }

        return pos;
    }

    /**
     * Calculates cosine similarity or how similar two vectors are
     * Useful for field of view calculations
     * <p>
     * NOTE: Both vectors must be normalized!
     * @param vectorA
     * @param vectorB
     * @return Value between -1 and 1, with 1 being exactly the same and -1 being the opposite direction
     */
    private static double cosineSimilarity(@NotNull Vec vectorA, @NotNull Vec vectorB) {
        return vectorA.x() * vectorB.x() + vectorA.y() * vectorB.y() + vectorA.z() * vectorB.z();
    }

    @Override
    protected void onCollide(@NotNull Entity entity, @NotNull Instance instance) {
        if (entity.getAliveTicks() <= 5) return; // probably still inside a block
        if (entity.getEntityType() != EntityType.FALLING_BLOCK) return;
        this.remove(entity);
    }

    private void remove(@NotNull Entity entity) {
        Block block = ((FallingBlockMeta) entity.getEntityMeta()).getBlock();
        entity.sendPacketToViewers(new WorldEventPacket(2001 /*Block break + block break sound*/, entity.getPosition(), block.stateId(), false));
        entity.remove();
    }
}
