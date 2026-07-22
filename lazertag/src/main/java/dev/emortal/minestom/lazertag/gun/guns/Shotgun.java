package dev.emortal.minestom.lazertag.gun.guns;

import dev.emortal.minestom.lazertag.game.LazerTagGame;
import dev.emortal.minestom.lazertag.gun.Gun;
import dev.emortal.minestom.lazertag.gun.GunItemInfo;
import dev.emortal.minestom.lazertag.gun.ItemRarity;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.ServerFlag;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

public final class Shotgun extends Gun {
    private static final GunItemInfo INFO = new GunItemInfo(
            Material.REPEATER,
            ItemRarity.LEGENDARY,

            1.25f,
            200,
            1,
            6,

            3300,
            400,
            0,
            0.13,
            20,

            Sound.sound(SoundEvent.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, Sound.Source.PLAYER, 1.5f, 1f)
    );

    public Shotgun(@NotNull LazerTagGame game) {
        super(game, "Shotgun", INFO);
    }

    @Override
    public void shoot(@NotNull Player shooter, int ammo) {
        super.shoot(shooter, ammo);

        shooter.scheduler()
                .buildTask(() -> this.playReloadSound(shooter))
                .delay(TaskSchedule.tick(6))
                .schedule();

        shooter.setVelocity(shooter.getPosition().direction().mul(-0.75 * ServerFlag.SERVER_TICKS_PER_SECOND));
    }
}
