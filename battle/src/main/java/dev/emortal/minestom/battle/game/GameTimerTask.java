package dev.emortal.minestom.battle.game;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.function.Supplier;

final class GameTimerTask implements Supplier<TaskSchedule> {
    private static final int INVULNERABILITY_TIME = 15;

    private final @NotNull BattleGame game;
    private final @NotNull BattleBossBar bossBar;

    private final int playTime;
    private final int glowing;
    private final int invulnerability;

    private int secondsLeft;

    GameTimerTask(@NotNull BattleGame game, @NotNull BattleBossBar bossBar) {
        this.game = game;
        this.bossBar = bossBar;

        this.playTime = this.calculatePlayTime();
        this.glowing = (int) Math.floor(this.playTime * 0.3);
        this.invulnerability = this.playTime - INVULNERABILITY_TIME;

        this.secondsLeft = this.playTime;
    }

    private int calculatePlayTime() {
        return 120 + (40 * this.game.getPlayers().size());
    }

    @Override
    public TaskSchedule get() {
        if (this.secondsLeft == 0) {
            this.game.victory(null);
            return TaskSchedule.stop();
        }

        if (this.secondsLeft <= 10) {
            this.doShowdownCountdown();
        }

        if (this.secondsLeft == this.glowing) {
            this.startGlowing();
        }

        if (this.secondsLeft >= this.invulnerability && this.secondsLeft <= this.invulnerability + INVULNERABILITY_TIME) {
            this.startInitialInvulnerability();
        }

        if (this.secondsLeft == this.invulnerability) {
            this.stopInvulnerability();
        }

        this.bossBar.updateProgress((float) this.secondsLeft / (float) this.playTime);
        this.secondsLeft--;
        return TaskSchedule.seconds(1);
    }

    private void stopInvulnerability() {
        this.game.sendActionBar(Component.text("You are no longer invulnerable"));
        this.game.playSound(Sound.sound(Key.key("battle.countdown.invulover"), Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self());

        for (Player player : this.game.getPlayers()) {
            player.setInvulnerable(false);
        }
    }

    private void startInitialInvulnerability() {
        int invulnerableSeconds = this.secondsLeft - this.invulnerability;
        this.game.sendActionBar(Component.text("You are invulnerable for " + invulnerableSeconds + " seconds"));

        if (invulnerableSeconds <= 5) {
            this.game.playSound(Sound.sound(Key.key("battle.countdown.begin"), Sound.Source.MASTER, 0.5f, 1.2f), Sound.Emitter.self());
        }
    }

    private void startGlowing() {
        this.game.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_HAT, Sound.Source.MASTER, 1f, 1.5f), Sound.Emitter.self());
        this.game.showTitle(Title.title(
                Component.empty(),
                Component.text("Everyone is now glowing!", NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(2))
        ));

        for (Player alive : this.game.getAlivePlayers()) {
            alive.setGlowing(true);
        }
    }

    private void doShowdownCountdown() {
        this.playShowdownCountdownSound();
        this.game.showTitle(Title.title(
                Component.empty(),
                Component.text(this.secondsLeft, TextColor.lerp(this.secondsLeft / 10f, NamedTextColor.RED, NamedTextColor.GREEN)),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
        ));
    }

    private void playShowdownCountdownSound() {
        Key soundKey = Key.key(Key.MINECRAFT_NAMESPACE, "battle.showdown.count" + ((this.secondsLeft % 2) + 1));
        this.game.playSound(Sound.sound(soundKey, Sound.Source.MASTER, 0.7f, 1f), Sound.Emitter.self());
    }
}
