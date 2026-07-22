package dev.emortal.minestom.lazertag.gun.guns;

import dev.emortal.minestom.lazertag.game.LazerTagGame;
import dev.emortal.minestom.lazertag.gun.Gun;
import dev.emortal.minestom.lazertag.gun.GunItemInfo;
import dev.emortal.minestom.lazertag.gun.ItemRarity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public final class GunGun extends Gun {
    private static final GunItemInfo INFO = new GunItemInfo(
            Material.STONE_HOE,
            ItemRarity.RARE,

            7f,
            200,
            0,
            15,

            2500,
            300,
            0,
            0,
            1,

            (a, pos) -> {
                ThreadLocalRandom current = ThreadLocalRandom.current();
                a.playSound(Sound.sound(Key.key("lazertag.pew"), Sound.Source.PLAYER, 1f, current.nextFloat(1.0f, 1.3f)), pos);
            }
    );

    public GunGun(LazerTagGame game) {
        super(game, "Gun Gun", INFO);
    }

    @Override
    public @NotNull ItemStack createItem() {
        return getCustomItem("emortalmc:lazertag/gungun");
    }
}
