package dev.emortal.minestom.blocksumo.powerup.item;

import dev.emortal.minestom.blocksumo.game.BlockSumoGame;
import dev.emortal.minestom.blocksumo.powerup.ItemRarity;
import dev.emortal.minestom.blocksumo.powerup.PowerUp;
import dev.emortal.minestom.blocksumo.powerup.PowerUpItemInfo;
import dev.emortal.minestom.blocksumo.powerup.SpawnLocation;
import dev.emortal.minestom.core.raycast.Ray;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class Switcheroo extends PowerUp {
    private static final Component NAME = MiniMessage.miniMessage().deserialize("<rainbow>Switcheroo</rainbow>");
    private static final PowerUpItemInfo ITEM_INFO = new PowerUpItemInfo(Material.ENDER_EYE, NAME, ItemRarity.LEGENDARY);

    public Switcheroo(@NotNull BlockSumoGame game) {
        super(game, "switcheroo", ITEM_INFO, SpawnLocation.CENTER);
    }

    @Override
    public void onUse(@NotNull Player player, @NotNull PlayerHand hand) {
        this.removeOneItemFromPlayer(player, hand);
        this.playSwitchSound(player);

        Pos eyePos = player.getPosition().add(0, player.getEyeHeight(), 0);
        Ray ray = new Ray(eyePos, player.getPosition().direction().mul(30));
        List<Ray.Intersection<Entity>> entities = ray.entitiesSorted(player.getInstance().getEntities());

        this.showSwitchParticle(player, eyePos);

        if (entities.isEmpty()) return;

        this.doSwitcheroo(player, entities.getFirst().object());
    }

    private void playSwitchSound(@NotNull Player player) {
        Sound sound = Sound.sound(SoundEvent.BLOCK_BEACON_ACTIVATE, Sound.Source.PLAYER, 1, 1);
        Pos source = player.getPosition();
        this.game.playSound(sound, source.x(), source.y(), source.z());
    }

    private void showSwitchParticle(@NotNull Player player, @NotNull Pos eyePosition) {
        Pos targetPos = eyePosition.add(player.getPosition().direction().mul(30));

        double step = 0.1;
        Pos direction = targetPos.sub(eyePosition).normalize().mul(step);

        Pos currentPos = eyePosition;
        for (int i = 0; i < eyePosition.distance(targetPos) * (1.0 / step); i++) {
            currentPos = currentPos.add(direction);

            ParticlePacket packet = new ParticlePacket(Particle.END_ROD, currentPos, Vec.ZERO, 0, 1);
            this.game.sendGroupedPacket(packet);
        }
    }

    private void doSwitcheroo(@NotNull Player player, @NotNull Entity target) {
        Pos targetPos = target.getPosition();
        Vec targetVelocity = target.getVelocity();

        Pos playerPos = player.getPosition();
        Vec playerVelocity = player.getVelocity();

        target.teleport(playerPos);
        target.setVelocity(playerVelocity);

        player.teleport(targetPos);
        player.setVelocity(targetVelocity);

        this.playTeleportSound(playerPos);
        this.playTeleportSound(targetPos);
    }

    private void playTeleportSound(@NotNull Point source) {
        Sound sound = Sound.sound(SoundEvent.ENTITY_ENDERMAN_TELEPORT, Sound.Source.PLAYER, 1, 1);
        this.game.playSound(sound, source.x(), source.y(), source.z());
    }
}
