package dev.emortal.minestom.lazertag.gun.guns;

import dev.emortal.minestom.lazertag.game.LazerTagGame;
import dev.emortal.minestom.lazertag.gun.Gun;
import dev.emortal.minestom.lazertag.gun.GunItemInfo;
import dev.emortal.minestom.lazertag.gun.ItemRarity;
import dev.emortal.minestom.lazertag.util.entity.BetterEntityProjectile;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.collision.Shape;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.ExplosionPacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.WeightedList;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class BlockChucker extends Gun {
    private static final GunItemInfo INFO = new GunItemInfo(
            Material.ANVIL,
            ItemRarity.LEGENDARY,

            10f,
            0,
            0,
            5,

            3000,
            500,
            0,
            0,
            1,

            Sound.sound(SoundEvent.BLOCK_BONE_BLOCK_BREAK, Sound.Source.PLAYER, 2f, 0.5f)
    );

    public BlockChucker(@NotNull LazerTagGame game) {
        super(game, "Block Chucker", INFO);

        BlockChuckerEntity.initBlocks();
    }

    @Override
    public void shoot(@NotNull Player shooter, int ammo) {
        BlockChuckerEntity entity = new BlockChuckerEntity(shooter);

        Pos spawnPos = shooter.getPosition().add(0, shooter.getEyeHeight() - EntityType.BEE.height() / 2, 0)
                .add(shooter.getPosition().direction().mul(1));

        entity.setInstance(this.game.getInstance(), spawnPos);
    }

    private final class BlockChuckerEntity extends BetterEntityProjectile {

        private static final List<Block> BLOCKS;

        static {
            List<Block> blocks = new ArrayList<>();

            for (Block value : Block.values()) {
                if (!value.solid()) continue;

                Shape shape = value.collisionShape();
                if (!shape.relativeStart().samePoint(0, 0, 0) || !shape.relativeEnd().samePoint(1, 1, 1)) continue;

                if (value.air()) continue;
                if (value.compare(Block.BARRIER)) continue;

                blocks.add(value);
            }

            BLOCKS = List.copyOf(blocks);
        }

        static void initBlocks() {
            // Will initialize the class and so initialize the blocks
        }

        private final float spinSpeed1;
        private final float spinSpeed2;

        public BlockChuckerEntity(@NotNull Player shooter) {
            super(shooter, EntityType.ITEM_DISPLAY);

            ThreadLocalRandom random = ThreadLocalRandom.current();
            spinSpeed1 = random.nextFloat(-0.3f, 0.3f);
            spinSpeed2 = random.nextFloat(-0.3f, 0.3f);

            editEntityMeta(ItemDisplayMeta.class, meta -> {
                meta.setItemStack(ItemStack.of(getRandomBlock().material()));
                meta.setPosRotInterpolationDuration(2);
                meta.setTransformationInterpolationDuration(2);
                meta.setTransformationInterpolationStartDelta(0);
            });

            super.setAerodynamics(getAerodynamics().withAirResistance(1.0, 1.0));
            super.setVelocity(shooter.getPosition().direction().mul(35));
            super.scheduleRemove(Duration.ofSeconds(5));
        }

        @Override
        public void tick(long time) {
            super.tick(time);

            editEntityMeta(ItemDisplayMeta.class, meta -> {
                Vec axis = new Vec(1, 0, 0).rotateAroundY(spinSpeed1 * 5);
                Quaternionf quaternion = new Quaternionf(new AxisAngle4f(getAliveTicks() * spinSpeed2, (float)axis.x(), (float)axis.y(), (float)axis.z()));

                meta.setTransformationInterpolationDuration(2);
                meta.setTransformationInterpolationStartDelta(0);
                meta.setLeftRotation(new float[] { quaternion.x, quaternion.y, quaternion.z, quaternion.w });
            });
        }

        private static @NotNull Block getRandomBlock() {
            int randomIndex = ThreadLocalRandom.current().nextInt(BLOCKS.size());
            return BLOCKS.get(randomIndex);
        }

        @Override
        public void collideBlock(@NotNull Point pos) {
            this.collide();
        }

        @Override
        public void collidePlayer(@NotNull Point pos, @NotNull Player player) {
            this.collide();
        }

        private void collide() {
            Pos pos = this.getPosition();
            // TODO: maybe merge the setvelocity below with the explosion packet
            ServerPacket explosionPacket = new ExplosionPacket(pos, 2f, 0, null, Particle.EXPLOSION_EMITTER, SoundEvent.ENTITY_GENERIC_EXPLODE, WeightedList.of());
            this.sendPacketToViewers(explosionPacket);

            for (Player victim : BlockChucker.this.game.getInstance().getPlayers()) {
                if (victim.isInvulnerable()) continue;
                if (victim.getDistanceSquared(this) > 5 * 5) continue;

                float damage = victim == this.shooter ? 5f : INFO.damage() / ((float) victim.getDistance(this) * 1.2f);
                BlockChucker.this.game.getDamageHandler().damage(victim, this.shooter, this.getPosition(), damage);

                victim.setVelocity(victim.getPosition().sub(this.getPosition().sub(0, 0.5, 0)).asVec().normalize().mul(10));
            }

            this.remove();
        }
    }
}
