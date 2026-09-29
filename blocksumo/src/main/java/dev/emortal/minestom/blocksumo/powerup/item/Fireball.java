package dev.emortal.minestom.blocksumo.powerup.item;

import dev.emortal.minestom.blocksumo.entity.BetterEntityProjectile;
import dev.emortal.minestom.blocksumo.explosion.ExplosionData;
import dev.emortal.minestom.blocksumo.game.BlockSumoGame;
import dev.emortal.minestom.blocksumo.powerup.ItemRarity;
import dev.emortal.minestom.blocksumo.powerup.PowerUp;
import dev.emortal.minestom.blocksumo.powerup.PowerUpItemInfo;
import dev.emortal.minestom.blocksumo.powerup.SpawnLocation;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.ServerFlag;
import net.minestom.server.collision.Aerodynamics;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.property.ServerProperties;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public final class Fireball extends PowerUp {
    private static final Component NAME = Component.text("Fireball", NamedTextColor.GOLD);
    private static final PowerUpItemInfo ITEM_INFO = new PowerUpItemInfo(Material.FIRE_CHARGE, NAME, ItemRarity.COMMON);

    private static final ExplosionData EXPLOSION = new ExplosionData(3, 35, 5.5, true);

    public Fireball(@NotNull BlockSumoGame game) {
        super(game, "fireball", ITEM_INFO, SpawnLocation.ANYWHERE);
    }

    @Override
    public void onUse(@NotNull Player player, @NotNull PlayerHand hand) {
        this.removeOneItemFromPlayer(player, hand);

        Entity fireball = this.shootFireball(player);
        Vec originalVelocity = player.getPosition().direction().mul(30);
        fireball.setVelocity(originalVelocity);

        this.playShootingSound(player.getPosition());

        fireball.scheduler().submitTask(new CleanupTask(fireball));
    }

    private @NotNull Entity shootFireball(@NotNull Player shooter) {
        Entity fireball = new FireballEntity(shooter);

        fireball.setInstance(this.game.getInstance(), shooter.getPosition().add(0, shooter.getEyeHeight(), 0));

        return fireball;
    }

    private void playShootingSound(@NotNull Point source) {
        Sound sound = Sound.sound(SoundEvent.ENTITY_GHAST_SHOOT, Sound.Source.BLOCK, 1, 1);
        this.game.playSound(sound, source.x(), source.y(), source.z());
    }

    private void collide(@NotNull Entity entity, @NotNull Player shooter) {
        this.game.getExplosionManager().explode(entity.getPosition(), EXPLOSION, shooter, entity);
        entity.remove();
    }

    @Override
    public void onUseOnBlock(@NotNull Player player, @NotNull PlayerHand hand) {
        this.onUse(player, hand);
    }

    @Override
    public boolean shouldHandleBlockPlace() {
        return true;
    }

    @Override
    public void onBlockPlace(@NotNull Player player, @NotNull PlayerHand hand, @NotNull Point clickedPos) {
        this.onUse(player, hand);
    }

    private final class FireballEntity extends BetterEntityProjectile {

        private final Player shooter;

        public FireballEntity(Player shooter) {
            super(shooter, EntityType.FIREBALL);

            this.shooter = shooter;

            setAerodynamics(new Aerodynamics(0.0, 1.0, 1.0));
            setNoGravity(true);
            setBoundingBox(0.6, 0.6, 0.6);
            setTag(PowerUp.NAME, Fireball.super.name);
        }

        @Override
        public void collideBlock(@NotNull Point pos) {
            Fireball.this.collide(this, shooter);
        }

        @Override
        public void collidePlayer(@NotNull Point pos, @NotNull Player player) {
            Fireball.this.collide(this, player);
        }
    }

    private final class CleanupTask implements Supplier<TaskSchedule> {

        private final @NotNull Entity fireball;

        CleanupTask(@NotNull Entity fireball) {
            this.fireball = fireball;
        }

        @Override
        public @NotNull TaskSchedule get() {
            if (this.fireball.getAliveTicks() > 5L * ServerProperties.SERVER_TICKS_PER_SECOND.get()) { // Remove if alive for longer than 5 seconds
                this.fireball.remove();
                return TaskSchedule.stop();
            }

            this.showTrailParticle(fireball);
            return TaskSchedule.nextTick();
        }

        private void showTrailParticle(@NotNull Entity fireball) {
            ParticlePacket packet = new ParticlePacket(Particle.LARGE_SMOKE, fireball.getPosition(), Vec.ZERO, 0.1f, 1);
            Fireball.this.game.sendGroupedPacket(packet);
        }
    }
}
