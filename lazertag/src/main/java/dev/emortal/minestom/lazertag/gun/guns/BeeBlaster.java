package dev.emortal.minestom.lazertag.gun.guns;

import dev.emortal.minestom.lazertag.game.DamageHandler;
import dev.emortal.minestom.lazertag.game.LazerTagGame;
import dev.emortal.minestom.lazertag.gun.Gun;
import dev.emortal.minestom.lazertag.gun.GunItemInfo;
import dev.emortal.minestom.lazertag.gun.ItemRarity;
import dev.emortal.minestom.lazertag.util.entity.BetterEntityProjectile;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.ExplosionPacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.WeightedList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public final class BeeBlaster extends Gun {
    private static final GunItemInfo INFO = new GunItemInfo(
            Material.HONEYCOMB,
            ItemRarity.LEGENDARY,

            70f,
            0,
            0,
            1,

            2000,
            2000,
            0,
            0,
            1,

            Sound.sound(SoundEvent.ENTITY_BEE_HURT, Sound.Source.PLAYER, 1.3f, 1.1f)
    );

    public BeeBlaster(@NotNull LazerTagGame game) {
        super(game, "Bee Blaster", INFO);
    }

    @Override
    public void shoot(@NotNull Player shooter, int ammo) {
        BeeBlasterEntity entity = new BeeBlasterEntity(shooter);

        Pos spawnPos = shooter.getPosition().add(0, shooter.getEyeHeight() - EntityType.BEE.height() / 2, 0)
                .add(shooter.getPosition().direction().mul(1));

        entity.setInstance(this.game.getInstance(), spawnPos);
    }

    private final class BeeBlasterEntity extends BetterEntityProjectile {

        BeeBlasterEntity(@NotNull Player shooter) {
            super(shooter, EntityType.BEE);

            super.setAerodynamics(getAerodynamics().withAirResistance(1.0, 1.0));
            super.setNoGravity(true);
            super.setVelocity(shooter.getPosition().direction().mul(25));
            super.scheduleRemove(Duration.ofSeconds(5));
        }

        @Override
        public void collideBlock(@NotNull Point pos) {
            this.collide(null);
        }

        @Override
        public void collidePlayer(@NotNull Point pos, @NotNull Player player) {
            this.collide(player);
        }

        private void collide(@Nullable Player collidedPlayer) {
            DamageHandler damageHandler = BeeBlaster.this.game.getDamageHandler();

            Pos pos = this.getPosition();
            // TODO: maybe merge the setvelocity below with the explosion packet
            ServerPacket explosionPacket = new ExplosionPacket(pos, 2f, 0, null, Particle.EXPLOSION_EMITTER, SoundEvent.ENTITY_GENERIC_EXPLODE, WeightedList.of(), true);
            this.sendPacketToViewers(explosionPacket);

            if (collidedPlayer != null) {
                damageHandler.damage(collidedPlayer, this.shooter, this.getPosition(), 20f);
            }

            for (Player victim : BeeBlaster.this.game.getPlayers()) {
                if (victim.isInvulnerable()) continue;
                if (victim.getDistanceSquared(this) > 5 * 5) continue;

                if (victim != this.shooter && victim.getDistanceSquared(this) < 1.7 * 1.7) {
                    damageHandler.damage(victim, this.shooter, this.getPosition(), 20f);
                    continue;
                }

                float damage = victim == this.shooter ? 5f : INFO.damage() / ((float) victim.getDistance(this) * 2.5f);
                damageHandler.damage(victim, this.shooter, this.getPosition(), damage);

                victim.setVelocity(victim.getPosition().sub(this.getPosition().sub(0, 0.5, 0)).asVec().normalize().mul(50));
            }

            this.remove();
        }
    }
}
