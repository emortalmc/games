package dev.emortal.minestom.lazertag.gun.guns;

import dev.emortal.minestom.lazertag.game.LazerTagGame;
import dev.emortal.minestom.lazertag.gun.Gun;
import dev.emortal.minestom.lazertag.gun.GunItemInfo;
import dev.emortal.minestom.lazertag.gun.ItemRarity;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

public final class AssaultRifle extends Gun {
    private static final GunItemInfo INFO = new GunItemInfo(
            Material.DIAMOND_HOE,
            ItemRarity.RARE,

            3.5f,
            200.0,
            2,
            40,

            3000,
            320,
            170,
            0,
            1,

            Sound.sound(SoundEvent.ENTITY_BLAZE_HURT, Sound.Source.PLAYER, 1.3f, 1.1f)
    );

    public AssaultRifle(@NotNull LazerTagGame game) {
        super(game, "Assault Rifle", INFO);
    }
}
