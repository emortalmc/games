package dev.emortal.minestom.lobby.emote;

import dev.emortal.bbstom.BBModel;
import dev.emortal.bbstom.renderer.BlockbenchPlayerRenderer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

public class Emote {

    private static final Path MODEL_PATH = Path.of("emotes.bbmodel");
    public static BBModel MODEL;

    private static final Tag<@NotNull EmoteTask> TASK_TAG = Tag.Transient("emoteTask");

    public static void init(EventNode<@NotNull Event> eventNode) {
        try {
            MODEL = BBModel.fromBytes(Files.readAllBytes(MODEL_PATH));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        eventNode.addListener(PlayerDisconnectEvent.class, e -> {
            EmoteTask task = e.getPlayer().getTag(TASK_TAG);
            if (task != null) {
                task.stop();
            }
        });
    }

    public static void stop(Player player) {
        EmoteTask task = player.getTag(TASK_TAG);
        if (task != null) {
            task.stop();
        }
    }

    public static void play(Player player, Type emote) {
        player.setInvisible(true);
        player.setAutoViewable(false);

        EmoteTask emoteTask = new EmoteTask(player, emote);
        player.setTag(TASK_TAG, emoteTask);
    }

    public static class EmoteTask implements Supplier<TaskSchedule> {
        private final Pos originalPos;
        private int ticks = 0;

        private boolean stop = false;

        private final Player player;
        private final Type emote;
        private final BlockbenchPlayerRenderer renderer;
        public EmoteTask(Player player, Type emote) {
            this.player = player;
            this.emote = emote;
            PlayerSkin skin = player.getSkin();
            if (skin == null) skin = PlayerSkin.fromUsername("emortaldev");
            this.renderer = new BlockbenchPlayerRenderer(MODEL, MODEL.getAnimationByName(emote.animationName), player.getInstance(), player.getPosition(), skin);

            this.originalPos = player.getPosition();

            Instance instance = player.getInstance();

            instance.scheduler().submitTask(this);
            this.renderer.render();
        }

        @Override
        public TaskSchedule get() {
            Instance instance = player.getInstance();

            renderer.setPosition(player.getPosition());

            if (!player.isOnline() || stop || !this.renderer.getTorso().getInstance().getUuid().equals(instance.getUuid())) {
                stop();
                return TaskSchedule.stop();
            }

            if (emote.soundTicks != 0) {
                if (ticks % emote.soundTicks == 0) {
                    instance.playSound(Sound.sound(emote.soundKey, Sound.Source.MASTER, 0.5f, 1f), player.getPosition());
                }
            }

            if (!emote.allowMovement) {
                if (originalPos.distanceSquared(player.getPosition()) > 1*1) {
                    stop();
                    return TaskSchedule.stop();
                }
            }

            ticks++;

            return TaskSchedule.tick(1);
        }

        public void stop() {
            player.setInvisible(false);
            player.setAutoViewable(true);
            if (emote.soundKey != null) player.getInstance().stopSound(SoundStop.named(emote.soundKey));
            renderer.remove();
            stop = true;
        }
    }

    private Emote() {

    }

    public enum Type {

        BOOGIE_DOWN("Boogie Down", Key.key("emote.song.boogiedown"), 357, false, "boogiedown"),
        GANGNAM_STYLE("Gangnam Style", Key.key("emote.song.gangnamstyle"), 286, false, "gangnam"),
        SKEDADDLE("Skedaddle", null, 0, true, "skedaddle"),
        REANIMATED("Reanimated", Key.key("emote.song.reanimated"), 194, false, "reanimated");

        private final String friendlyName;
        private final Key soundKey;
        private final int soundTicks;
        private final boolean allowMovement;
        private final String animationName;

        Type(String friendlyName, Key soundKey, int soundTicks, boolean allowMovement, String animationName) {
            this.friendlyName = friendlyName;
            this.soundKey = soundKey;
            this.soundTicks = soundTicks;
            this.allowMovement = allowMovement;
            this.animationName = animationName;
        }

        public String getFriendlyName() {
            return friendlyName;
        }

        public Key getSoundKey() {
            return soundKey;
        }

        public int getSoundTicks() {
            return soundTicks;
        }

        public boolean getAllowMovement() {
            return allowMovement;
        }
    }


}
