package dev.emortal.minestom.holeymoley.game;

import dev.emortal.minestom.holeymoley.event.Event;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.minestom.server.ServerFlag;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.function.Supplier;

final class RegularEventTask implements Supplier<TaskSchedule> {
    private static final int EVENT_REPEAT = 40;
    private static final int INITIAL_EVENT_DELAY = 30;

    private final @NotNull HoleyMoleyGame game;

    private boolean first = true;

    RegularEventTask(@NotNull HoleyMoleyGame game) {
        this.game = game;
    }

    @Override
    public TaskSchedule get() {
        if (first) {
            first = false;
            return TaskSchedule.tick(ServerFlag.SERVER_TICKS_PER_SECOND * INITIAL_EVENT_DELAY);
        }

        startEvent();

        return TaskSchedule.tick(ServerFlag.SERVER_TICKS_PER_SECOND * EVENT_REPEAT);
    }

    private void startEvent() {
        Event event = this.game.getEventManager().createRandomEvent();
        event.doEvent(this.game);

        this.game.playSound(Sound.sound(SoundEvent.ENTITY_ENDER_DRAGON_GROWL, Sound.Source.MASTER, 0.6f, 2f));

        this.game.showTitle(
                Title.title(
                        MiniMessage.miniMessage().deserialize("<rainbow><bold>" + event.getName()),
                        Component.empty(),
                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
                )
        );

        this.game.sendMessage(
                Component.text()
                        .append(Component.text("★", NamedTextColor.GOLD))
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(Component.text("New event! ", NamedTextColor.GRAY))
                        .append(Component.text(event.getName(), NamedTextColor.GOLD))
        );
    }

}
