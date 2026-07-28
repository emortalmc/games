package dev.emortal.minestom.holeymoley.game;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.ServerFlag;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

final class RegularGlowTask implements Supplier<TaskSchedule> {
    private static final int GLOW_REPEAT = 40;
    private static final int INITIAL_GLOW_DELAY = 15;
    private static final int WARN_GLOW = 3;
    private static final int GLOW_DURATION = 4;

    private final @NotNull HoleyMoleyGame game;

    private boolean first = true;
    private int warnSeconds = WARN_GLOW;

    RegularGlowTask(@NotNull HoleyMoleyGame game) {
        this.game = game;
    }

    @Override
    public TaskSchedule get() {
        if (first) {
            first = false;
            return TaskSchedule.tick(ServerFlag.SERVER_TICKS_PER_SECOND * INITIAL_GLOW_DELAY);
        }

        if (warnSeconds > 0) {
            if (warnSeconds == WARN_GLOW) {
                this.game.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 1f, 0.5f));
            } else {
                this.game.playSound(Sound.sound(SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_OFF, Sound.Source.MASTER, 1f, 1.2f));
            }
            sendWarnGlowingMessage(warnSeconds);
            warnSeconds--;
            return TaskSchedule.tick(ServerFlag.SERVER_TICKS_PER_SECOND);
        }

        startGlowing();
        warnSeconds = WARN_GLOW;

        return TaskSchedule.tick(ServerFlag.SERVER_TICKS_PER_SECOND * GLOW_REPEAT);
    }

    private void sendWarnGlowingMessage(int seconds) {
        this.game.sendMessage(
                Component.text()
                        .append(Component.text("⚠", NamedTextColor.RED))
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(Component.text("Players glow in ", NamedTextColor.GRAY))
                        .append(Component.text(seconds, NamedTextColor.RED))
                        .append(Component.space())
                        .append(Component.text(seconds == 1 ? "second" : "seconds", NamedTextColor.GRAY))
        );
    }

    private void startGlowing() {
        this.game.sendMessage(
                Component.text()
                        .append(Component.text("⚠", NamedTextColor.RED))
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(Component.text("Players are now glowing!", NamedTextColor.RED))
        );

        this.game.playSound(Sound.sound(SoundEvent.BLOCK_ANVIL_LAND, Sound.Source.MASTER, 0.5f, 1f));

        for (Player alive : this.game.getAlivePlayers()) {
            alive.setGlowing(true);
        }

        this.game.getInstance().scheduler().buildTask(this::stopGlowing)
                .delay(TaskSchedule.tick(ServerFlag.SERVER_TICKS_PER_SECOND * GLOW_DURATION))
                .schedule();
    }

    private void stopGlowing() {
        this.game.playSound(Sound.sound(SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_OFF, Sound.Source.MASTER, 1f, 1.2f));

        for (Player alive : this.game.getAlivePlayers()) {
            alive.setGlowing(false);
        }
    }

}
