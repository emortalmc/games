package dev.emortal.minestom.lobby.gadget.blaster;

import dev.emortal.minestom.lobby.LobbyTags;
import dev.emortal.minestom.lobby.gadget.BlockChangingGadget;
import dev.emortal.minestom.lobby.util.SphereUtil;
import dev.emortal.minestom.lobby.util.WorldBlock;
import dev.emortal.minestom.lobby.util.entity.BetterEntityProjectile;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityProjectile;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.item.SnowballMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class InkBlaster extends BlockChangingGadget {
    private static final Set<Point> BLOCKS_IN_SPHERE = SphereUtil.getBlocksInSphere(2.0);
    private static final ItemStack ITEM = ItemStack.builder(Material.WHITE_CANDLE)
            .set(DataComponents.CUSTOM_NAME, Component.text("Ink Blaster").decoration(TextDecoration.ITALIC, false))
            .build();
    private static final ItemStack[] INK_COLORS = Arrays.stream(InkColor.values()).map(color -> ItemStack.of(color.getDye())).collect(Collectors.toSet()).toArray(new ItemStack[0]);
    private static final Block[] BLOCK_COLORS = Arrays.stream(InkColor.values()).map(color -> blockFromMaterial(color.getDye())).collect(Collectors.toSet()).toArray(new Block[0]);
    private static final Tag<Integer> MARKER_TAG = Tag.Integer("ink_blaster");

    private static final double SPREAD = 0.1;
    private static final double BULLETS = 4;

    public InkBlaster() {
        super("Ink Blaster", ITEM, "ink_blaster", true);
    }

    @Override
    protected void onUse(@NotNull Player user, @NotNull Instance instance) {
        user.playSound(Sound.sound(SoundEvent.ENTITY_GHAST_SHOOT, Sound.Source.MASTER, 0.5F, 2F), Sound.Emitter.self());

        for (int i = 0; i < BULLETS; i++) {
            Vec eyeDirection = calculateEyeDirection(user);
            Vec velocity = eyeDirection.mul(26.0);

            this.spawnInkBall(instance, user, velocity);
        }
    }

    private @NotNull Vec calculateEyeDirection(@NotNull Player user) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        // We take the direction the user is facing and add random factors to it to spread the ink balls out, to give a spray effect
        return user.getPosition().direction()
                .rotateAroundX(random.nextDouble(-SPREAD, SPREAD))
                .rotateAroundY(random.nextDouble(-SPREAD, SPREAD))
                .rotateAroundZ(random.nextDouble(-SPREAD, SPREAD));
    }

    private void spawnInkBall(@NotNull Instance instance, @NotNull Player player, @NotNull Vec velocity) {
        BetterEntityProjectile inkBall = new BetterEntityProjectile(player, EntityType.SNOWBALL);

        SnowballMeta meta = (SnowballMeta) inkBall.getEntityMeta();
        int colorIndex = ThreadLocalRandom.current().nextInt(INK_COLORS.length);
        meta.setItem(INK_COLORS[colorIndex]);

        inkBall.setVelocity(velocity);
        inkBall.setTag(LobbyTags.LOBBABLE, true);
        inkBall.setTag(MARKER_TAG, colorIndex);

        inkBall.setInstance(instance, player.getPosition().add(0, player.getEyeHeight(), 0));
        inkBall.scheduleRemove(10, ChronoUnit.SECONDS);
    }

    @Override
    protected void onCollide(@NotNull Entity entity, @NotNull Instance instance) {
        Integer markerTag = entity.getTag(MARKER_TAG);
        if (markerTag == null) return;

        Block block = BLOCK_COLORS[markerTag];

        List<WorldBlock> nearbyBlocks = SphereUtil.getNearbyBlocks(entity.getPosition(), BLOCKS_IN_SPHERE, instance,
                b -> b.solid() && !b.compare(Block.OAK_WALL_SIGN));

        for (WorldBlock nearbyBlock : nearbyBlocks) {
            BlockChangePacket packet = new BlockChangePacket(nearbyBlock.position(), block);
            instance.sendGroupedPacket(packet);
        }

        Player shooter = (Player) ((EntityProjectile) entity).getShooter();
        shooter.playSound(Sound.sound(SoundEvent.ENTITY_DONKEY_CHEST, Sound.Source.MASTER, 4F, 2F), entity.getPosition());

        super.regenerateBlocks(instance, nearbyBlocks, b -> {
            BlockChangePacket packet = new BlockChangePacket(b.position(), b.block());
            instance.sendGroupedPacket(packet);
        });

        entity.remove();
    }

    private static @NotNull Block blockFromMaterial(@NotNull Material material) {
        return Block.fromKey(material.name().replace("dye", "wool"));
    }
}
