package dev.emortal.minestom.lazertag.gun.guns;

import dev.emortal.minestom.lazertag.game.LazerTagGame;
import dev.emortal.minestom.lazertag.gun.Gun;
import dev.emortal.minestom.lazertag.gun.GunItemInfo;
import dev.emortal.minestom.lazertag.gun.ItemRarity;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

public final class Minigun extends Gun {
    private static final GunItemInfo INFO = new GunItemInfo(
            Material.NETHERITE_SHOVEL,
            ItemRarity.RARE,

            0.75f,
            200,
            3,
            70,

            3500,
            100,
            50,
            0,
            1,

            Sound.sound(SoundEvent.BLOCK_BEACON_DEACTIVATE, Sound.Source.PLAYER, 2f, 2f)
    );

    public Minigun(@NotNull LazerTagGame game) {
        super(game, "Lazer Minigun", INFO);
    }
}
