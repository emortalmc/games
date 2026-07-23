package dev.emortal.minestom.lobby.features;

import dev.emortal.messaging.RedisMessenger;
import dev.emortal.minestom.core.EmortalServer;
import dev.emortal.minestom.lobby.util.SignUtils;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ClickySignFeature implements LobbyFeature {

    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();
    private static final Point SIGN_POS = new Vec(9, 66, -17);
    private static final String REDIS_KEY = "lobby:clicky-sign:count";

    private static final Component COMPONENT_TEXT_1 = Component.text("This sign has", NamedTextColor.BLACK);
    private static final Component COMPONENT_TEXT_2 = Component.text("been clicked", NamedTextColor.BLACK);
    private static final Component COMPONENT_TEXT_4 = Component.text("times", TextColor.color(212, 11, 212));

    private long count = 0;

    @Override
    public void register(@NotNull Instance instance) {
        instance.loadChunk(SIGN_POS).join();
        RedisMessenger redis = EmortalServer.getRedis();

        instance.eventNode().addListener(PlayerBlockInteractEvent.class, event -> {
            if (!event.getBlockPosition().sameBlock(SIGN_POS)) return;

            event.getPlayer().playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_HAT, Sound.Source.BLOCK, 0.75f, 2f), Sound.Emitter.self());
            updateSign(instance, ++count);
            redis.increment(REDIS_KEY);
        });

        redis.get(REDIS_KEY).thenAccept(value -> {
            count = value == null ? 0 : Long.parseLong(value);
            instance.scheduler().scheduleNextTick(() -> updateSign(instance, count));
        });
    }

    private void updateSign(@NotNull Instance instance, long count) {
        Component componentText3 = Component.text(count, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD);

        CompoundBinaryTag signData = SignUtils.createNBT(false, "black", List.of(COMPONENT_TEXT_1, COMPONENT_TEXT_2, componentText3, COMPONENT_TEXT_4));

        instance.setBlock(
                SIGN_POS,
                Block.BIRCH_WALL_SIGN
                        .withProperty("facing", "south")
                        .withTag(Tag.NBT("front_text"), signData)
        );
    }
}
