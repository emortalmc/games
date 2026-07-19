package dev.emortal.minestom.lazertag.game;

import dev.emortal.minestom.core.game.Game;
import dev.emortal.minestom.core.game.config.GameCreationInfo;
import dev.emortal.minestom.core.game.util.GameWinLoseMessages;
import dev.emortal.minestom.core.map.LoadedMap;
import dev.emortal.minestom.lazertag.gun.GunManager;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public final class LazerTagGame extends Game {
    private static final Pos WAITING_SPAWN_POINT = new Pos(0, 64, 0);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public static final int KILLS_TO_WIN = 20;

    private final @NotNull GunManager gunManager;
    private final @NotNull DamageHandler damageHandler;
    private final @NotNull ScoreboardHandler scoreboardHandler;

    public LazerTagGame(@NotNull GameCreationInfo creationInfo, @NotNull LoadedMap map) {
        super(creationInfo, map);

        this.gunManager = new GunManager(this);
        this.damageHandler = new DamageHandler(this, map);
        this.scoreboardHandler = new ScoreboardHandler(this);
    }

    @Override
    public void onPreJoin(@NotNull Player player) {
        player.setRespawnPoint(WAITING_SPAWN_POINT);
    }

    @Override
    public void onJoin(@NotNull Player player) {
        player.scheduleNextTick((p) -> {
            player.setFlying(false);
            player.setAllowFlying(false);
            player.setAutoViewable(true);
            player.setTeam(null);
            player.setGlowing(false);
            player.setInvulnerable(false);
            player.setGameMode(GameMode.ADVENTURE);
        });
    }

    @Override
    public void onLeave(@NotNull Player player) {
        player.setTeam(null);
        player.clearEffects();
        TagHandler.removePlayerTags(player);

        if (this.getPlayers().size() <= 1) {
            this.victory();
        }
    }

    @Override
    public void start() {
        for (Player player : this.getPlayers()) {
            TagHandler.initializePlayerTags(player);
            player.setHeldItemSlot((byte) 4);
            this.damageHandler.respawn(player);
            this.scoreboardHandler.show(player);
        }

        this.gunManager.registerListeners();
        this.damageHandler.registerListeners();
    }

    @Override
    public @NotNull Instance getSpawningInstance(@NotNull Player player) {
        return getMap().instance();
    }

    public @NotNull Instance getInstance() {
        return getMap().instance();
    }

    public void victory() {
//        if (gameTimerTask != null) gameTimerTask.cancel();

        Title victoryTitle = Title.title(
                MINI_MESSAGE.deserialize("<gradient:#ffc570:gold><bold>VICTORY!"),
                Component.text(GameWinLoseMessages.randomVictory(), NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(3))
        );
        Title defeatTitle = Title.title(
                MINI_MESSAGE.deserialize("<gradient:#ff474e:#ff0d0d><bold>DEFEAT!"),
                Component.text(GameWinLoseMessages.randomDefeat(), NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(3))
        );

        Sound defeatSound = Sound.sound(SoundEvent.ENTITY_VILLAGER_NO, Sound.Source.MASTER, 1f, 1f);
        Sound victorySound = Sound.sound(SoundEvent.BLOCK_BEACON_POWER_SELECT, Sound.Source.MASTER, 1f, 0.8f);

        // Choose the player with the highest kills
        Player highestKiller = this.findPlayerWithHighestKills();

        for (Player player : this.getPlayers()) {
            if (highestKiller == player) {
                player.showTitle(victoryTitle);
                player.playSound(victorySound, Sound.Emitter.self());
            } else {
                player.showTitle(defeatTitle);
                player.playSound(defeatSound, Sound.Emitter.self());
            }
        }

        getMap().instance().scheduler().buildTask(this::finish)
                .delay(TaskSchedule.seconds(6))
                .schedule();
    }

    private @Nullable Player findPlayerWithHighestKills() {
        int killsRecord = 0;
        Player highestKiller = null;

        for (Player player : this.getPlayers()) {
            Integer playerKills = player.getTag(DamageHandler.KILLS_TAG);
            if (playerKills == null) playerKills = 0;
            if (playerKills > killsRecord) {
                killsRecord = playerKills;
                highestKiller = player;
            }
        }

        return highestKiller;
    }

    @Override
    public void cleanUp() {
        getMap().instance().scheduleNextTick(MinecraftServer.getInstanceManager()::unregisterInstance);
    }

    public @NotNull DamageHandler getDamageHandler() {
        return this.damageHandler;
    }

    public @NotNull GunManager getGunManager() {
        return this.gunManager;
    }

    public @NotNull ScoreboardHandler getScoreboardHandler() {
        return this.scoreboardHandler;
    }
}
