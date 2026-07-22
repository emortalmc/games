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

import java.util.HexFormat;

public final class TVFeature implements LobbyFeature {

    private static final Vec SOUND_POS = new Vec(-37.5, 52, 6);
    private static final Pos TV_SCREEN_POS = new Pos(-36.98, 54, 6.0, 90f, 0f);
    private static final int AUDIO_LENGTH_TICKS = 286;

    @Override
    public void register(@NotNull Instance instance) {
        BetterEntity tvScreen = new BetterEntity(EntityType.TEXT_DISPLAY);
        tvScreen.setTicking(false);
        tvScreen.setPhysics(false);
        tvScreen.editEntityMeta(TextDisplayMeta.class, meta -> {
            meta.setBackgroundColor(0);
            meta.setScale(new Vec(0.9));
        });

        instance.scheduler().buildTask(new Runnable() {
            int i = 0;
            final int startHex = HexFormat.fromHexDigits("E000");

            @Override
            public void run() {
                i++;
                if (i > 100) {
                    i = 0;

                    instance.playSound(Sound.sound(Key.key("entity.cat.chilllikethat"), Sound.Source.MASTER, 1f, 1f), SOUND_POS);
                }

                char hex = (char) (startHex + i);

                tvScreen.editEntityMeta(TextDisplayMeta.class, meta -> {
                    meta.setText(Component.text(hex).font(Key.key("cat")));
                });
            }
        }).repeat(TaskSchedule.tick(2)).schedule();

        tvScreen.setInstance(instance, TV_SCREEN_POS.withYaw(-90));
    }
}
