package dev.emortal.minestom.blocksumo.explosion;

import dev.emortal.minestom.blocksumo.game.BlockSumoGame;
import dev.emortal.minestom.blocksumo.game.PlayerTags;
import dev.emortal.minestom.blocksumo.team.TeamColor;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.metadata.other.PrimedTntMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.ExplosionPacket;
import net.minestom.server.network.packet.server.play.HitAnimationPacket;
import net.minestom.server.network.packet.server.play.WorldEventPacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.WeightedList;
import net.minestom.server.utils.position.PositionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class ExplosionManager {

    private final @NotNull BlockSumoGame game;
    private final @NotNull Instance instance;

    public ExplosionManager(@NotNull BlockSumoGame game) {
        this.game = game;
        this.instance = game.getInstance();
    }

    public @NotNull Entity spawnTnt(@NotNull Point origin, int fuseTime, @NotNull ExplosionData data, @Nullable Player placer) {
        Entity tnt = new Entity(EntityType.TNT);

        PrimedTntMeta meta = (PrimedTntMeta) tnt.getEntityMeta();
        meta.setFuseTime(fuseTime);

        tnt.setVelocity(new Vec(0.4, 4, 0).rotateAroundY(ThreadLocalRandom.current().nextDouble(Math.PI*2)));

        tnt.setInstance(this.instance, origin);
        this.playPrimedSound(tnt);

        tnt.scheduler()
                .buildTask(() -> this.explodeTntAndRemove(tnt, data, placer))
                .delay(TaskSchedule.tick(fuseTime))
                .schedule();
        return tnt;
    }

    private void playPrimedSound(@NotNull Entity tnt) {
        Sound sound = Sound.sound(SoundEvent.ENTITY_TNT_PRIMED, Sound.Source.BLOCK, 2, 1);
        this.game.playSound(sound, tnt);
    }

    private void explodeTntAndRemove(@NotNull Entity tnt, @NotNull ExplosionData data, @Nullable Player placer) {
        this.explode(tnt.getPosition(), data, placer, tnt);
        tnt.remove();
    }

    public void explode(@NotNull Point origin, @NotNull ExplosionData data, @Nullable Player source, @NotNull Entity entity) {
        this.doExplosionDamage(origin, data, source, entity);
        this.game.sendGroupedPacket(new ExplosionPacket(origin, 2f, 0, null, Particle.EXPLOSION_EMITTER, SoundEvent.ENTITY_GENERIC_EXPLODE, WeightedList.of()));

        if (!data.breakBlocks()) return;
        this.explodeBlocks(origin, data);
    }

    private void doExplosionDamage(@NotNull Point position, @NotNull ExplosionData explosion, @Nullable Player source, @NotNull Entity entity) {
        UUID sourceId = source == null ? null : source.getUuid();
        TeamColor sourceTeam = source == null ? null : source.getTag(PlayerTags.TEAM_COLOR);

        double forceDistance = explosion.forceDistance() * explosion.forceDistance();
        for (Player player : this.game.getPlayers()) {
            if (player.getGameMode() != GameMode.SURVIVAL) continue;
            if (!this.canBeHarmedBySource(player, sourceId, sourceTeam)) continue;

            double distance = player.getPosition().distanceSquared(position);
            if (distance > forceDistance) continue;

            Point direction = player.getPosition().sub(position);
            float yaw = PositionUtils.getLookYaw(direction.x(), direction.z());
            this.game.sendGroupedPacket(new HitAnimationPacket(player.getEntityId(), yaw));

            player.damage(Damage.fromEntity(entity, 0));
            Vec newVelocity = player.getPosition()
                    .sub(position.sub(0, 1, 0))
                    .asVec()
                    .normalize()
                    .mul(explosion.force());
            player.setVelocity(newVelocity);
        }
    }

    private boolean canBeHarmedBySource(@NotNull Player target, @Nullable UUID sourceId, @Nullable TeamColor sourceColor) {
        return target.getUuid().equals(sourceId) ||
                !this.game.getSpawnProtectionManager().isProtected(target) && // Target does not have spawn protection
                !target.getTag(PlayerTags.TEAM_COLOR).equals(sourceColor); // Target is not on the same team as source
    }

    private void explodeBlocks(@NotNull Point origin, @NotNull ExplosionData explosion) {
        AbsoluteBlockBatch batch = new AbsoluteBlockBatch();
        List<Point> blocksToBreak = getBlocksInSphere(explosion.size());

        for (Point pos : blocksToBreak) {
            Point blockPos = origin.add(pos);

            Block block = this.instance.getBlock(blockPos, Block.Getter.Condition.TYPE);
            if (!this.canExplodeBlock(block)) continue;

            // Send block break effect because Minestom won't send it for a batch break.
            this.game.sendGroupedPacket(new WorldEventPacket(2001, blockPos, block.stateId(), false));

            batch.setBlock(blockPos, Block.AIR);
        }

        batch.apply(this.instance, null);
    }

    private boolean canExplodeBlock(@NotNull Block block) {
        // We can only explode wool blocks and air. This avoids us exploding bedrock, the diamond block, or any of the actual map.
        return block.name().toLowerCase(Locale.ROOT).contains("wool") || block.air();
    }

    private static @NotNull List<Point> getBlocksInSphere(int radius) {
        List<Point> blocks = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    int distance = x * x + y * y + z * z;
                    if (distance > radius * radius) continue;

                    blocks.add(new Vec(x, y, z));
                }
            }
        }

        return blocks;
    }
}
