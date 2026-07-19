package dev.emortal.minestom.blocksumo.game;

import com.alibaba.fastjson2.JSONObject;
import dev.emortal.messaging.types.BlockSumoData;
import dev.emortal.minestom.blocksumo.event.EventManager;
import dev.emortal.minestom.blocksumo.explosion.ExplosionManager;
import dev.emortal.minestom.blocksumo.powerup.PowerUpManager;
import dev.emortal.minestom.blocksumo.scoreboard.ScoreboardManager;
import dev.emortal.minestom.blocksumo.spawning.InitialSpawnPointSelector;
import dev.emortal.minestom.blocksumo.spawning.PlayerRespawnHandler;
import dev.emortal.minestom.blocksumo.spawning.SpawnProtectionManager;
import dev.emortal.minestom.core.game.Game;
import dev.emortal.minestom.core.game.config.GameCreationInfo;
import dev.emortal.minestom.core.game.util.GameWinLoseMessages;
import dev.emortal.minestom.core.map.LoadedMap;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class BlockSumoGame extends Game {
    public static final @NotNull Pos CENTER = new Pos(0.5, 65, 0.5);
    public static final @NotNull Component TITLE =
            MiniMessage.miniMessage().deserialize("<gradient:blue:aqua><bold>Block Sumo</bold></gradient>");

    private final @NotNull PlayerManager playerManager;
    private final @NotNull PlayerRespawnHandler respawnHandler;
    private final @NotNull SpawnProtectionManager spawnProtectionManager;
    private final @NotNull PlayerDisconnectHandler disconnectHandler;
    private final @NotNull EventManager eventManager;
    private final @NotNull PowerUpManager powerUpManager;
    private final @NotNull InitialSpawnPointSelector initialSpawnPointSelector;
    private final @NotNull ExplosionManager explosionManager;
    private final @NotNull Map<UUID, BlockSumoData> playerDataMap;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean ended = new AtomicBoolean(false);

    private @Nullable Task countdownTask;

    // end of game data used to pass to the game tracker
    private Set<Player> winners;

    public BlockSumoGame(@NotNull GameCreationInfo creationInfo, @NotNull LoadedMap map, @NotNull Map<UUID, BlockSumoData> playerData) {
        super(creationInfo, map);
        this.playerDataMap = playerData;

        int spawnRadius = map.data().getIntValue("spawnRadius");

        this.respawnHandler = new PlayerRespawnHandler(this, spawnRadius);

        this.playerManager = new PlayerManager(this, respawnHandler, new ScoreboardManager(), 49);
        this.spawnProtectionManager = new SpawnProtectionManager();
        this.disconnectHandler = new PlayerDisconnectHandler(this, this.playerManager, respawnHandler, this.spawnProtectionManager);

        this.eventManager = new EventManager(this);
        this.eventManager.registerDefaultEvents();

        this.powerUpManager = new PowerUpManager(this);
        this.powerUpManager.registerDefaultPowerUps();
        this.initialSpawnPointSelector = new InitialSpawnPointSelector(creationInfo.playerIds().size(), spawnRadius);
        this.explosionManager = new ExplosionManager(this);

        this.playerManager.registerPreGameListeners(super.getEventNode());
    }

    @Override
    public void onPreJoin(@NotNull Player player) {
        player.setRespawnPoint(this.initialSpawnPointSelector.select());
    }

    @Override
    public void onJoin(@NotNull Player player) {
        player.setAutoViewable(true);
        this.playerManager.addInitialTags(player);
        this.playerManager.getTeamManager().allocateTeam(player);
    }

    @Override
    public void onLeave(@NotNull Player player) {
        this.disconnectHandler.onDisconnect(player);
    }

    @Override
    public void start() {
        this.countdownTask = getMap().instance().scheduler().submitTask(new Supplier<>() {
            int i = 5;

            @Override
            public @NotNull TaskSchedule get() {
                if (this.i == 3) {
                    BlockSumoGame.this.playSound(Sound.sound(SoundEvent.BLOCK_PORTAL_TRIGGER, Sound.Source.MASTER, 0.45f, 1.27f));
                }

                if (this.i == 0) {
                    BlockSumoGame.this.showGameStartTitle();
                    BlockSumoGame.this.startGame();
                    return TaskSchedule.stop();
                }

                BlockSumoGame.this.showCountdown(this.i);
                this.i--;
                return TaskSchedule.seconds(1);
            }
        });
    }

    private void startGame() {
        started.set(true);

        this.playerManager.registerGameListeners(this.getEventNode());
        this.powerUpManager.registerListeners(this.getEventNode());
        this.removeLockingEntities();

        for (Player player : this.getPlayers()) {
            this.respawnHandler.giveItems(player);
            this.setSpawnBlockToWool(player);
        }

        this.eventManager.startRandomEventTask();
        this.powerUpManager.startRandomPowerUpTasks();
    }

    private void showCountdown(final int countdown) {
        this.playSound(Sound.sound(Key.key("battle.countdown.begin"), Sound.Source.MASTER, 1F, 1F), Sound.Emitter.self());
        this.showTitle(Title.title(
                Component.text(countdown, NamedTextColor.GREEN, TextDecoration.BOLD),
                Component.empty(),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(500))
        ));
    }

    private void showGameStartTitle() {
//        Title title = Title.title(
//                Component.text("GO!", NamedTextColor.GREEN, TextDecoration.BOLD),
//                Component.empty(),
//                Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ZERO)
//        );
//        this.showTitle(title);
        this.clearTitle();
    }

    private void removeLockingEntities() {
        for (Entity entity : getMap().instance().getEntities()) {
            if (entity.getEntityType() == EntityType.AREA_EFFECT_CLOUD) entity.remove();
        }
    }

    private void setSpawnBlockToWool(@NotNull Player player) {
        Pos pos = player.getPosition();
        getMap().instance().setBlock(pos.blockX(), pos.blockY() - 1, pos.blockZ(), Block.WHITE_WOOL);
    }

    public void cancelCountdown() {
        if (this.countdownTask != null) this.countdownTask.cancel();
    }

    public void victory(@NotNull Set<Player> winners) {
        if (this.hasEnded()) return;
        this.ended.set(true);
        this.winners = winners;

        Sound victorySound = Sound.sound(SoundEvent.ENTITY_VILLAGER_CELEBRATE, Sound.Source.MASTER, 1f, 1f);
        Sound victorySound2 = Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1f, 1f);
        Sound defeatSound = Sound.sound(SoundEvent.ENTITY_VILLAGER_DEATH, Sound.Source.MASTER, 1f, 0.8f);

        Title victoryTitle = Title.title(
                MiniMessage.miniMessage().deserialize("<gradient:#ffc570:gold><bold>VICTORY!</bold></gradient>"),
                Component.text(GameWinLoseMessages.randomVictory(), NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(4))
        );
        Title defeatTitle = Title.title(
                MiniMessage.miniMessage().deserialize("<gradient:#ff474e:#ff0d0d><bold>DEFEAT!</bold></gradient>"),
                Component.text(GameWinLoseMessages.randomDefeat(), NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(4))
        );

        this.sendMessage(createVictoryMessage(winners));

        for (Player player : this.getPlayers()) {
            if (winners.contains(player)) {
                player.showTitle(victoryTitle);
                player.playSound(victorySound);
                player.playSound(victorySound2);
            } else {
                player.showTitle(defeatTitle);
                player.playSound(defeatSound);
            }
        }

        getEventManager().getRandomEventHandler().stopRandomEventTask();

        getMap().instance().scheduler().buildTask(this::finish).delay(TaskSchedule.seconds(6)).schedule();
    }

    private Component createVictoryMessage(@NotNull Set<Player> winners) {
        TextComponent.Builder message = Component.text();

        message.append(Component.text(" ".repeat(61), NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH));
        message.append(Component.newline());
        message.append(Component.newline());

        message.append(MiniMessage.miniMessage().deserialize("<gradient:#ffc570:gold><bold>VICTORY</bold></gradient>"));
        int i = 0;
        for (Player winner : winners) {
            if (i > 0) message.append(Component.text(","));
            message.append(Component.space());
            message.append(Component.text(winner.getUsername()));
            i++;
        }

        message.append(Component.text("\n\nKill leaderboard:\n", NamedTextColor.WHITE));

        // Copy the players and sort them by kills (descending)
        List<Player> playersCopy = new ArrayList<>(this.getPlayers());
        playersCopy.sort(Comparator.comparingInt(player -> -player.getTag(PlayerTags.KILLS)));

        int j = 0;
        for (Player player : playersCopy) {
            j++;
            Style style = switch (j) {
                case 1 -> Style.style(NamedTextColor.GOLD);
                case 2 -> Style.style(TextColor.color(210, 210, 210));
                case 3 -> Style.style(TextColor.color(205, 127, 50));
                default -> Style.style(TextColor.color(140, 140, 140));
            };

            message.append(Component.text(j + ". ", j <= 3 ? NamedTextColor.WHITE : NamedTextColor.GRAY));
            message.append(Component.text(player.getUsername(), style));
            message.append(Component.text(" - ", NamedTextColor.DARK_GRAY));
            message.append(Component.text(player.getTag(PlayerTags.KILLS), j <= 3 ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.WHITE));
            message.append(Component.newline());
        }

        message.append(Component.newline());
        message.append(Component.text(" ".repeat(61), NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH));

        return message.build();
    }

    @Override
    public void cleanUp() {
        getMap().instance().scheduleNextTick(MinecraftServer.getInstanceManager()::unregisterInstance);
        this.playerManager.cleanUp();
    }

    @Override
    public @NotNull Instance getSpawningInstance(@NotNull Player player) {
        return getMap().instance();
    }

    public @NotNull Instance getInstance() {
        return getMap().instance();
    }

    public @NotNull Map<UUID, BlockSumoData> getPlayerDataMap() {
        return this.playerDataMap;
    }

    public @NotNull PlayerManager getPlayerManager() {
        return this.playerManager;
    }

    public @NotNull EventManager getEventManager() {
        return this.eventManager;
    }

    public @NotNull PowerUpManager getPowerUpManager() {
        return this.powerUpManager;
    }

    public @NotNull ExplosionManager getExplosionManager() {
        return this.explosionManager;
    }

    public @NotNull SpawnProtectionManager getSpawnProtectionManager() {
        return this.spawnProtectionManager;
    }

    public @NotNull JSONObject mapData() {
        return getMap().data();
    }

    public boolean hasEnded() {
        return this.ended.get();
    }

    public boolean hasStarted() {
        return this.started.get();
    }


    public void setRespawnRadius(int respawnRadius) {
        this.respawnHandler.getRespawnPointSelector().setSpawnRadius(respawnRadius);
    }
}
