package dev.emortal.minestom.lobby.gadget;

import dev.emortal.minestom.lobby.LobbyTags;
import dev.emortal.minestom.lobby.features.SeatingFeature;
import dev.emortal.minestom.lobby.util.SphereUtil;
import dev.emortal.minestom.lobby.util.WorldBlock;
import dev.emortal.minestom.lobby.util.entity.BetterEntityProjectile;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.collision.Aerodynamics;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityProjectile;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ExplosionPacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.WeightedList;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

public final class Fireball extends BlockChangingGadget {
    private static final Set<Point> BLOCKS_IN_SPHERE = SphereUtil.getBlocksInSphere(3.0);
    private static final ItemStack ITEM = ItemStack.builder(Material.FIRE_CHARGE)
            .set(DataComponents.CUSTOM_NAME, Component.text("Fireball").decoration(TextDecoration.ITALIC, false))
            .build();

    public Fireball() {
        super("Fireball", ITEM, "fireball", true);
    }

    @Override
    protected void onUse(@NotNull Player user, @NotNull Instance instance) {
        user.playSound(Sound.sound(SoundEvent.ENTITY_GHAST_SHOOT, Sound.Source.MASTER, 0.7F, 1.2F), Sound.Emitter.self());

        // Shoot in the direction the user is facing with a speed of 38
        Vec velocity = user.getPosition().direction().mul(38.0);
        this.shootFireball(user, instance, velocity);
    }

    private void shootFireball(@NotNull Player shooter, @NotNull Instance instance, @NotNull Vec direction) {
        BetterEntityProjectile fireball = new BetterEntityProjectile(shooter, EntityType.FIREBALL);

        fireball.setNoGravity(true);
        fireball.setVelocity(direction);
        fireball.setTag(LobbyTags.LOBBABLE, true);
        fireball.setAerodynamics(new Aerodynamics(0.0, 1.0, 1.0));

        fireball.setInstance(instance, shooter.getPosition().add(0, shooter.getEyeHeight(), 0));
        fireball.scheduleRemove(10, ChronoUnit.SECONDS);
    }

    @Override
    protected void onCollide(@NotNull Entity entity, @NotNull Instance instance) {
        if (entity.getEntityType() != EntityType.FIREBALL) return;

        Point pos = entity.getPosition();
        List<WorldBlock> blocks = SphereUtil.getNearbyBlocks(pos, BLOCKS_IN_SPHERE, instance, block -> !block.air());
        this.explodeBlocks(instance, blocks);

        super.regenerateInstanceBlocks(instance, blocks);
        entity.remove();

        Player shooter = (Player) ((EntityProjectile) entity).getShooter();
        if (shooter == null) return;
        shooter.sendPacket(new ExplosionPacket(pos, 2f, 0, null, Particle.EXPLOSION_EMITTER, SoundEvent.ENTITY_GENERIC_EXPLODE, WeightedList.of()));
    }

    private void explodeBlocks(@NotNull Instance instance, @NotNull List<WorldBlock> blocks) {
        AbsoluteBlockBatch batch = new AbsoluteBlockBatch();

        for (WorldBlock block : blocks) {
            Point position = block.position();
            batch.setBlock(position, Block.AIR);

            Entity seat = SeatingFeature.armorStandSeats.get(position);
            if (seat != null) seat.remove();
        }

        batch.apply(instance, null);
    }
}
