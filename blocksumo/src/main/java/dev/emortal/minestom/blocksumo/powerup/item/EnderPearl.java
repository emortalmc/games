package dev.emortal.minestom.blocksumo.powerup.item;

import dev.emortal.minestom.blocksumo.entity.BetterEntityProjectile;
import dev.emortal.minestom.blocksumo.game.BlockSumoGame;
import dev.emortal.minestom.blocksumo.powerup.ItemRarity;
import dev.emortal.minestom.blocksumo.powerup.PowerUp;
import dev.emortal.minestom.blocksumo.powerup.PowerUpItemInfo;
import dev.emortal.minestom.blocksumo.powerup.SpawnLocation;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

public final class EnderPearl extends PowerUp {
    private static final Component NAME = MiniMessage.miniMessage().deserialize("<gradient:blue:light_purple>Ender Pearl</gradient>");
    private static final PowerUpItemInfo ITEM_INFO = new PowerUpItemInfo(Material.ENDER_PEARL, NAME, ItemRarity.RARE);

    public EnderPearl(@NotNull BlockSumoGame game) {
        super(game, "ender_pearl", ITEM_INFO, SpawnLocation.CENTER);
    }

    @Override
    public void onUse(@NotNull Player player, @NotNull PlayerHand hand) {
        this.removeOneItemFromPlayer(player, hand);
        this.shootProjectile(player);
        this.playThrowSound(player);
    }

    private void shootProjectile(@NotNull Player shooter) {
        EnderPearlEntity pearl = new EnderPearlEntity(shooter);

        Instance instance = shooter.getInstance();
        pearl.setInstance(instance, shooter.getPosition().add(0, shooter.getEyeHeight(), 0));
    }

    private void playThrowSound(@NotNull Player thrower) {
        Sound sound = Sound.sound(SoundEvent.ENTITY_ENDER_PEARL_THROW, Sound.Source.PLAYER, 1, 1);
        Pos source = thrower.getPosition();
        this.game.playSound(sound, source.x(), source.y(), source.z());
    }

    private void playCollideSound(@NotNull Player thrower) {
        Sound sound = Sound.sound(SoundEvent.ENTITY_PLAYER_TELEPORT, Sound.Source.PLAYER, 1, 1);
        Pos source = thrower.getPosition();
        this.game.playSound(sound, source.x(), source.y(), source.z());
    }

    private void onCollide(@NotNull Player shooter, @NotNull Point collisionPosition) {
        Pos shooterPos = shooter.getPosition();
        shooter.teleport(collisionPosition.asPos(shooterPos.yaw(), shooterPos.pitch()));
        playCollideSound(shooter);
    }

    private final class EnderPearlEntity extends BetterEntityProjectile {
        private final Player shooter;
        public EnderPearlEntity(@NotNull Player shooter) {
            super(shooter, EntityType.ENDER_PEARL);

            this.shooter = shooter;

            setTag(PowerUp.NAME, EnderPearl.super.name);
            setBoundingBox(0.25, 0.25, 0.25);
            setVelocity(shooter.getPosition().direction().mul(35.0));
            setAerodynamics(getAerodynamics().withGravity(0.04));
        }

        @Override
        public void collidePlayer(@NotNull Point pos, @NotNull Player player) {
            onCollide(shooter, pos);
            remove();
        }

        @Override
        public void collideBlock(@NotNull Point pos) {
            onCollide(shooter, pos.add(0, 1, 0));
            remove();
        }
    }

}
