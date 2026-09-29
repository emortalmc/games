package dev.emortal.minestom.lazertag.gun.guns;

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
import net.minestom.server.entity.metadata.animal.tameable.CatMeta;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.ExplosionPacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.WeightedList;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public final class Catipult extends Gun {
    private static final GunItemInfo INFO = new GunItemInfo(
            Material.CAT_SPAWN_EGG,
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

            Sound.sound(SoundEvent.ENTITY_CAT_AMBIENT, Sound.Source.PLAYER, 1f, 1.5f)
    );

    public Catipult(@NotNull LazerTagGame game) {
        super(game, "Catipult", INFO);
    }

    @Override
    public void shoot(@NotNull Player shooter, int ammo) {
        CatipultEntity entity = new CatipultEntity(shooter);

        Pos spawnPos = shooter.getPosition().add(0, shooter.getEyeHeight() - EntityType.CAT.height() / 2, 0)
                .add(shooter.getPosition().direction().mul(1));

        entity.setInstance(this.game.getInstance(), spawnPos);
    }

    private final class CatipultEntity extends BetterEntityProjectile {

//        private static final CatMeta.Variant[] VARIANTS = MinecraftServer.process().catVariant().values();

        public CatipultEntity(@NotNull Player shooter) {
            super(shooter, EntityType.CAT);

            ThreadLocalRandom random = ThreadLocalRandom.current();

//            set(DataComponents.CAT_VARIANT, CatVariant.ALL_BLACK);
            editEntityMeta(CatMeta.class, meta -> {
                meta.setTamed(true);
            });

            super.setAerodynamics(getAerodynamics().withAirResistance(1.0, 1.0));
            super.setVelocity(shooter.getPosition().direction().mul(35));
            super.scheduleRemove(Duration.ofSeconds(5));
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
            ServerPacket explosionPacket = new ExplosionPacket(pos, 2f, 0, null, Particle.EXPLOSION_EMITTER, SoundEvent.ENTITY_GENERIC_EXPLODE, WeightedList.of(), true);
            this.sendPacketToViewers(explosionPacket);

            for (Player victim : Catipult.this.game.getInstance().getPlayers()) {
                if (victim.isInvulnerable()) continue;
                if (victim.getDistanceSquared(this) > 5 * 5) continue;

                float damage = victim == this.shooter ? 5f : INFO.damage() / ((float) victim.getDistance(this) * 1.2f);
                Catipult.this.game.getDamageHandler().damage(victim, this.shooter, this.getPosition(), damage);

                victim.setVelocity(victim.getPosition().sub(this.getPosition().sub(0, 0.5, 0)).asVec().normalize().mul(10));
            }

            this.remove();
        }
    }
}
