package dev.emortal.minestom.blocksumo.event.events;

import dev.emortal.minestom.blocksumo.explosion.ExplosionData;
import dev.emortal.minestom.blocksumo.game.BlockSumoGame;
import dev.emortal.minestom.blocksumo.powerup.item.HotPotato;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.ServerFlag;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.property.ServerProperties;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public final class HotPotatoEvent implements BlockSumoEvent {
    private static final Component START_MESSAGE = Component.text()
            .append(Component.text("Uh oh...", NamedTextColor.RED))
            .append(Component.text(" you'd better avoid that flaming potato!", NamedTextColor.YELLOW))
            .build();

    private final BossBar bossBar = BossBar.bossBar(Component.text("Hot Potato Event", NamedTextColor.RED), 1f, BossBar.Color.RED, BossBar.Overlay.NOTCHED_20);

    private final @NotNull BlockSumoGame game;
    private final @NotNull HotPotato hotPotatoPowerup;

    public HotPotatoEvent(@NotNull BlockSumoGame game) {
        this.game = game;
        this.hotPotatoPowerup = new HotPotato(game);
    }

    @Override
    public void start() {
        this.game.showBossBar(bossBar);

        Player randomPlayer = getRandomPlayer();
        if (randomPlayer == null) return;
        this.game.getPowerUpManager().givePowerUp(randomPlayer, this.hotPotatoPowerup);
        this.hotPotatoPowerup.sendWarningMessage(randomPlayer);
        randomPlayer.setTag(HotPotato.HOT_POTATO_HOLDER_TAG, true);

        startTimer();
    }

    private @Nullable Player getRandomPlayer() {
        List<Player> playersCopy = new ArrayList<>(this.game.getPlayers().size());
        for (Player player : this.game.getPlayers()) { // Filter out dead players
            if (player.getGameMode() != GameMode.SURVIVAL) continue;
            playersCopy.add(player);
        }
        if (playersCopy.isEmpty()) return null;

        return playersCopy.get(ThreadLocalRandom.current().nextInt(playersCopy.size()));
    }

    private void startTimer() {
        this.game.getInstance().scheduler().submitTask(new Supplier<>() {
            int secondsLeft = 20;

            @Override
            public TaskSchedule get() {
                HotPotatoEvent.this.game.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_HAT, Sound.Source.MASTER, 1f, 2f), Sound.Emitter.self());

                if (getHotPotatoHolder() == null) { // If someone died with the hot potato, give it to someone else
                    Player randomPlayer = getRandomPlayer();
                    if (randomPlayer == null) return TaskSchedule.tick(5);
                    HotPotatoEvent.this.game.getPowerUpManager().givePowerUp(randomPlayer, HotPotatoEvent.this.hotPotatoPowerup);
                    HotPotatoEvent.this.hotPotatoPowerup.sendWarningMessage(randomPlayer);
                    randomPlayer.setTag(HotPotato.HOT_POTATO_HOLDER_TAG, true);
                }

                if (secondsLeft == 0) {
                    explode();
                    return TaskSchedule.stop();
                }

                bossBar.progress(secondsLeft / 20f);

                secondsLeft--;

                return TaskSchedule.tick(ServerProperties.SERVER_TICKS_PER_SECOND.get());
            }
        });
    }

    private void explode() {
        Player hotPotatoHolder = getHotPotatoHolder();
        if (hotPotatoHolder != null) {
            this.game.getPlayerManager().getDeathHandler().kill(hotPotatoHolder, null);
            // harmless explosion
            this.game.getExplosionManager().explode(hotPotatoHolder.getPosition(), new ExplosionData(2, 0.0, 0.0, false), null, hotPotatoHolder);
        }

        for (Entity entity : this.game.getInstance().getEntities()) {
            if (entity instanceof HotPotato.HotPotatoEntity) entity.remove();
        }

        this.game.hideBossBar(bossBar);
    }

    private @Nullable Player getHotPotatoHolder() {
        for (Player player : this.game.getPlayers()) {
            if (player.hasTag(HotPotato.HOT_POTATO_HOLDER_TAG)) {
                return player;
            }
        }
        return null;
    }

    @Override
    public @NotNull Component getStartMessage() {
        return START_MESSAGE;
    }

}
