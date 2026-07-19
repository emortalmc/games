package dev.emortal.minestom.battle.game;

import com.alibaba.fastjson2.JSONObject;
import dev.emortal.minestom.battle.listeners.PvpListener;
import dev.emortal.minestom.core.game.Game;
import dev.emortal.minestom.core.game.config.GameCreationInfo;
import dev.emortal.minestom.core.game.util.GameWinLoseMessages;
import dev.emortal.minestom.core.map.LoadedMap;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class BattleGame extends Game {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final BattleBossBar bossBar = new BattleBossBar();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean ended = new AtomicBoolean();

    private @Nullable Task gameTimerTask;

    public BattleGame(@NotNull GameCreationInfo creationInfo, @NotNull LoadedMap map) {
        super(creationInfo, map);
    }

    @Override
    public void onPreJoin(@NotNull Player player) {
        JSONObject data = getMap().data();
        Vec circleCenter = data.getObject("circleCenter", Vec.class);
        double circleRadius = data.getDoubleValue("circleRadius");
        player.setRespawnPoint(circleCenter.add(0, 0, -circleRadius).asPos());
    }

    @Override
    public void onJoin(@NotNull Player player) {
//        player.setFlying(false);
//        player.setAllowFlying(true);
        player.setAutoViewable(true);
        player.setTeam(PlayerTeams.ALIVE);
        player.setGlowing(false);
        player.setGameMode(GameMode.ADVENTURE);
//        player.setGameMode(GameMode.SPECTATOR);
    }

    @Override
    public void onLeave(@NotNull Player player) {
        player.setTeam(null);
        player.clearEffects();

        this.checkPlayerCounts();
    }

    @Override
    public void start() {
        this.started.set(true);

        GameStartHandler startHandler = new GameStartHandler(this, getMap());
        startHandler.freezePlayers();
        this.gameTimerTask = startHandler.createTimerTask(this.bossBar);
    }

    void beginTimer() {
        this.gameTimerTask = getMap().instance().scheduler().submitTask(new GameTimerTask(this, this.bossBar));
    }

    public void checkPlayerCounts() {
        Set<Player> alivePlayers = this.getAlivePlayers();

        if (alivePlayers.isEmpty()) {
            this.finish();
            return;
        }

        if (alivePlayers.size() == 1) {
            if (this.started.get()) {
                this.victory(alivePlayers.iterator().next());
            } else {
                this.finish();
            }

            return;
        }

        // TODO: update player count in a scoreboard
        this.bossBar.updateRemaining(alivePlayers.size());
    }

    public void victory(@Nullable Player winner) {
        this.ended.set(true);

        if (this.gameTimerTask != null) {
            this.gameTimerTask.cancel();
        }

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

        // If there is no winner, choose the player with the highest kills
        if (winner == null) {
            winner = this.findPlayerWithHighestKills();
        }

        for (Player player : this.getPlayers()) {
            player.setInvulnerable(true);
            this.bossBar.hide(player);

            if (winner == player) {
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
            Integer playerKills = player.getTag(PvpListener.KILLS_TAG);
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
        this.bossBar.delete();
    }

    @Override
    public @NotNull Instance getSpawningInstance(@NotNull Player player) {
        return getMap().instance();
    }

    public @NotNull Instance getInstance() {
        return getMap().instance();
    }

    public @NotNull Set<Player> getAlivePlayers() {
        Set<Player> alivePlayers = ConcurrentHashMap.newKeySet();
        for (Player player : getPlayers()) {
            if (player.getGameMode() != GameMode.ADVENTURE) continue;
            alivePlayers.add(player);
        }
        return Collections.unmodifiableSet(alivePlayers);
    }

    public boolean hasEnded() {
        return this.ended.get();
    }
}
