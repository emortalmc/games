package dev.emortal.minestom.lobby.features;

import dev.emortal.minestom.lobby.util.PolarConvertingLoader;
import dev.emortal.minestom.lobby.util.entity.BetterEntity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.minestom.server.color.Color;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.position.PositionUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public final class ABSSecretFeature implements LobbyFeature {
    private static final Logger LOGGER = LoggerFactory.getLogger(ABSSecretFeature.class);

    public static final Tag<Boolean> DOOR_UNLOCKED_TAG = Tag.Boolean("absDoorUnlocked");
    private static final Tag<Boolean> TELEPORTING_TAG = Tag.Boolean("absTeleporting");

    private static final Title FADE_TITLE = Title.title(
            Component.text("\uE000"),
            Component.empty(),
            Title.Times.times(Duration.ofMillis(1700), Duration.ofMillis(50), Duration.ofMillis(400))
    );
    private static final Pos WORLD_SPAWN_POS = new Pos(-0.5, 50, -6.5);
    private static final Pos SIGN_POS = new Pos(46, 66, 0);
    public static final Pos DOOR_TOP_POS = new Pos(47, 66, 0);
    public static final Pos DOOR_BOTTOM_POS = new Pos(47, 65, 0);

    private SecretWorld secretWorld;

    @Override
    public void register(@NotNull Instance instance) {
        secretWorld = new SecretWorld();

        instance.setBlock(SIGN_POS, Block.AIR);

        instance.eventNode().addListener(PlayerMoveEvent.class, e -> {
            if (!e.getPlayer().hasTag(DOOR_UNLOCKED_TAG)) return;
            if (e.getPlayer().hasTag(TELEPORTING_TAG)) return;

            Pos pos = e.getNewPosition();
            if (pos.x() > 47 && pos.x() < 49 && pos.y() > 64 && pos.y() < 66 && pos.z() > 0 && pos.z() < 1) {
                secretWorld.goToSecretWorld(e.getPlayer());
            }
        });
    }

    private static final class SecretWorld {
        private final Instance instance;

        public SecretWorld() {
            PolarConvertingLoader loader = new PolarConvertingLoader("absworld");
            instance = loader.load().join();

            instance.enableAutoChunkLoad(false);
            for (int x = -2; x < 2; x++) {
                for (int y = -2; y < 2; y++) {
                    instance.loadChunk(x, y);
                }
            }

            BetterEntity textEntity = new BetterEntity(EntityType.TEXT_DISPLAY);
            textEntity.setTicking(false);
            textEntity.editEntityMeta(TextDisplayMeta.class, meta -> {
                meta.setScale(new Vec(3));
                meta.setText(Component.text("ᴀ\nʙᴇᴀᴄᴏɴ\nѕᴄʜᴏᴏʟ"));
                meta.setBackgroundColor(0);
                meta.setAlignLeft(true);
            });
            textEntity.setInstance(instance, new Pos(6 - Vec.EPSILON, 42, -7, 90f, 0f));


            Vec wireEndPos = new Vec(2.5, 44, 1);
            Vec wireStartPos = new Vec(-2.5, 44, 2.5);
            Vec diff = wireEndPos.sub(wireStartPos);
            float yaw = PositionUtils.getLookYaw(diff.x(), diff.z());

            BetterEntity chestplate = new BetterEntity(EntityType.ITEM_DISPLAY);
            chestplate.setTicking(false);
            chestplate.editEntityMeta(ItemDisplayMeta.class, meta -> {
                meta.setItemStack(ItemStack.of(Material.LEATHER_CHESTPLATE).with(DataComponents.DYED_COLOR, new Color(255, 0, 0)));
            });
            chestplate.setInstance(instance, lerpVec(wireEndPos, wireStartPos, 0.8f).asPos().add(0, 0.2, 0).withYaw(yaw + 90));

            BetterEntity wireEnd = new BetterEntity(EntityType.CHICKEN);
            wireEnd.setInvisible(true);
            wireEnd.setTicking(false);

            BetterEntity wireStart = new BetterEntity(EntityType.LEASH_KNOT);
            wireStart.setTicking(false);
            wireEnd.setLeashHolder(wireStart);

            wireEnd.setInstance(instance, wireEndPos);
            wireStart.setInstance(instance, wireStartPos);
        }

        public void goToSecretWorld(Player player) {
            player.setTag(TELEPORTING_TAG, true);
            player.showTitle(FADE_TITLE);
            player.scheduler().buildTask(() -> {
                player.setInstance(instance, WORLD_SPAWN_POS).thenRun(() -> {
                    player.removeTag(TELEPORTING_TAG);
                });
            }).delay(TaskSchedule.tick(1700 / 50)).schedule();

            player.scheduler().buildTask(() -> {
                player.sendMessage(Component.text("Use /spawn to get back to the lobby.", NamedTextColor.AQUA));
            }).delay(TaskSchedule.tick((1700 / 50) + 4 * 20)).schedule();

            player.scheduler().buildTask(() -> {
                instance.playSound(Sound.sound(Key.key("song.abs.clear"), Sound.Source.MASTER, 0.1f, 1f), WORLD_SPAWN_POS);
                instance.playSound(Sound.sound(Key.key("song.abs.muffled"), Sound.Source.MASTER, 0.08f, 1f), Sound.Emitter.self());
            }).delay(TaskSchedule.tick(1700 / 50 + 20)).repeat(TaskSchedule.tick(17 * 20)).schedule();
        }
    }

    private static Vec lerpVec(Vec a, Vec b, double f) {
        return new Vec(
                lerp(a.x(), b.x(), f),
                lerp(a.y(), b.y(), f),
                lerp(a.z(), b.z(), f)
        );
    }

    private static double lerp(double a, double b, double f) {
        return a + f * (b - a);
    }

}
