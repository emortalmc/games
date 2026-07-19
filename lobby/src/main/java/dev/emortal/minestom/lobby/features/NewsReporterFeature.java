package dev.emortal.minestom.lobby.features;

import dev.emortal.minestom.lobby.util.entity.BetterEntity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

public final class NewsReporterFeature implements LobbyFeature {

    private static final Vec SOUND_POS = new Vec(-37.5, 52, 6);
    private static final Pos TV_SCREEN_POS = new Pos(-36.98, 54.3, 6.0, 90f, 0f);
    private static final int AUDIO_LENGTH_TICKS = 286;

    @Override
    public void register(@NotNull Instance instance) {
        BetterEntity tvScreen = new BetterEntity(EntityType.TEXT_DISPLAY);
        tvScreen.setTicking(false);
        tvScreen.setPhysics(false);
        tvScreen.editEntityMeta(TextDisplayMeta.class, meta -> {
            meta.setText(Component.text("\uE01B\uF001\uE01C"));
            meta.setBackgroundColor(0);
            meta.setScale(new Vec(0.9));
        });
        tvScreen.setInstance(instance, TV_SCREEN_POS.withYaw(-90));

        instance.scheduler().buildTask(() -> {
            instance.playSound(Sound.sound(Key.key("entity.roblox.reporter"), Sound.Source.MASTER, 1f, 1f), SOUND_POS);
        }).repeat(TaskSchedule.tick(AUDIO_LENGTH_TICKS)).schedule();
    }
}
