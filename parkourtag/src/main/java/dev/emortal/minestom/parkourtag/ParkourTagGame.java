package dev.emortal.minestom.parkourtag;

import com.alibaba.fastjson2.JSONObject;
import dev.emortal.minestom.core.game.Game;
import dev.emortal.minestom.core.game.config.GameCreationInfo;
import dev.emortal.minestom.core.game.util.GameWinLoseMessages;
import dev.emortal.minestom.core.map.LoadedMap;
import dev.emortal.minestom.parkourtag.listeners.ParkourTagAttackListener;
import dev.emortal.minestom.parkourtag.listeners.ParkourTagDoubleJumpListener;
import dev.emortal.minestom.parkourtag.listeners.ParkourTagTickListener;
import dev.emortal.minestom.parkourtag.physics.MinecraftPhysics;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.color.TeamColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.AreaEffectCloudMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class ParkourTagGame extends Game {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Pos SPAWN_POINT = new Pos(0.5, 65.0, 0.5);

    public static final Team TAGGER_TEAM = MinecraftServer.getTeamManager().createBuilder("taggers")
            .teamColor(TeamColor.RED)
            .nameTagVisibility(TeamsPacket.NameTagVisibility.ALWAYS)
            .collisionRule(TeamsPacket.CollisionRule.NEVER)
            .updateTeamPacket()
            .build();
    public static final Team GOONS_TEAM = MinecraftServer.getTeamManager().createBuilder("goons")
            .teamColor(TeamColor.GREEN)
            .nameTagVisibility(TeamsPacket.NameTagVisibility.HIDE_FOR_OTHER_TEAMS)
            .collisionRule(TeamsPacket.CollisionRule.NEVER)
            .updateTeamPacket()
            .build();
    public static final Team DEAD_TEAM = MinecraftServer.getTeamManager().createBuilder("dead")
            .teamColor(TeamColor.GRAY)
            .prefix(Component.text("☠ ", NamedTextColor.GRAY))
            .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
            .collisionRule(TeamsPacket.CollisionRule.NEVER)
            .updateTeamPacket()
            .build();

    public static final int MIN_PLAYERS = 2;

    private final MinecraftPhysics physics;

    private final Set<Player> taggers = ConcurrentHashMap.newKeySet();
    private final Set<Player> goons = ConcurrentHashMap.newKeySet();
    private final BossBar bossBar = BossBar.bossBar(Component.empty(), 0f, BossBar.Color.PINK, BossBar.Overlay.PROGRESS);

    private GameStage gameStage = GameStage.PRE_GAME;
    private @Nullable Task gameTimerTask;

    protected ParkourTagGame(@NotNull GameCreationInfo creationInfo, @NotNull LoadedMap map, MinecraftPhysics physics) {
        super(creationInfo, map);
        this.physics = physics;

        ParkourTagTickListener.registerListener(this.getEventNode(), this, this.getInstance());
    }

    @Override
    public void onPreJoin(Player player) {
        player.setRespawnPoint(SPAWN_POINT);
    }

    @Override
    public void onJoin(Player player) {
        player.setFlying(false);
        player.setAllowFlying(false);
        player.setAutoViewable(true);
        player.setTeam(null);
        player.setGlowing(false);
        player.setGameMode(GameMode.ADVENTURE);
    }

    @Override
    public void onLeave(@NotNull Player player) {
        player.setFlying(false);
        player.setAllowFlying(false);
        player.setAutoViewable(true);
        player.setTeam(null);
        player.setGlowing(false);
        player.setGameMode(GameMode.ADVENTURE);

        this.checkPlayerCounts();
    }

    @Override
    public @NotNull Instance getSpawningInstance(@NotNull Player player) {
        return getMap().instance();
    }

    public @NotNull Instance getInstance() {
        return getMap().instance();
    }

    public void start() {
        this.playSound(Sound.sound(SoundEvent.BLOCK_PORTAL_TRIGGER, Sound.Source.MASTER, 0.45f, 1.27f));

        getMap().instance().scheduler().submitTask(new Supplier<>() {
            int i = 3;

            @Override
            public TaskSchedule get() {
                if (i == 0) {
                    pickTagger();
                    return TaskSchedule.stop();
                }

                playSound(Sound.sound(Key.key("battle.countdown.begin"), Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self());

                showTitle(
                        Title.title(
                                Component.empty(),
                                Component.text(i, NamedTextColor.LIGHT_PURPLE),
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(500))
                        )
                );

                i--;
                return TaskSchedule.seconds(1);
            }
        });
    }

    private void pickTagger() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        getMap().instance().scheduler().submitTask(new Supplier<>() {
            int nameIter = 15;
            final int offset = random.nextInt(ParkourTagGame.this.getPlayers().size());

            @Override
            public TaskSchedule get() {
                Iterator<Player> iterator = getPlayers().iterator();
                int playerIndex = (nameIter + offset) % getPlayers().size();
                for (int playerIter = 0; playerIter < playerIndex; playerIter++) {
                    iterator.next();
                }
                Player player = iterator.next();

                if (nameIter == 0) {

                    playSound(Sound.sound(SoundEvent.ENTITY_ENDER_DRAGON_GROWL, Sound.Source.MASTER, 0.8f, 1f), Sound.Emitter.self());

                    // fancy rainbow name animation
                    getMap().instance().scheduler().submitTask(new Supplier<>() {
                        int i = 0;

                        @Override
                        public TaskSchedule get() {
                            if (i >= 30) {
                                // finally begin the game!!
                                player.setTeam(TAGGER_TEAM);
                                taggers.add(player);

                                beginGame();
                                return TaskSchedule.stop();
                            }

                            showTitle(
                                    Title.title(
                                            MINI_MESSAGE.deserialize("<rainbow:" + i + ">" + player.getUsername()),
                                            Component.text("is the tagger", NamedTextColor.GRAY),
                                            Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ZERO)
                                    )
                            );
                            i++;
                            return TaskSchedule.tick(2);
                        }
                    });

                    return TaskSchedule.stop();
                }

                playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_HAT, Sound.Source.MASTER, 1f, 1.5f));

                showTitle(
                        Title.title(
                                Component.text(player.getUsername()),
                                Component.empty(),
                                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ZERO)
                        )
                );

                nameIter--;

                return TaskSchedule.tick(Math.max(1, (int) ((17 - nameIter) / 1.2)));
            }
        });
    }

    private void beginGame() {
        clearTitle();

        Set<Player> taggers = getTaggers();

        int goonsLeft = this.getPlayers().size() - 1;
        this.bossBar.name(
                Component.text()
                        .append(Component.text(goonsLeft, TextColor.fromHexString("#cdffc4"), TextDecoration.BOLD)) // assumes only one tagger
                        .append(Component.text(goonsLeft == 1 ? " goon remaining" : " goons remaining", TextColor.fromHexString("#8fff82")))
                        .build()
        );
        this.bossBar.color(BossBar.Color.GREEN);

        var holderEntity = new Entity(EntityType.AREA_EFFECT_CLOUD);
        holderEntity.setNoGravity(true);
        ((AreaEffectCloudMeta) holderEntity.getEntityMeta()).setRadius(0f);

        JSONObject data = getMap().data();
        Pos taggerSpawn = data.getObject("tagger", Pos.class);
        Pos goonSpawn = data.getObject("goon", Pos.class);
        holderEntity.setInstance(getMap().instance(), taggerSpawn.add(0, 0.1, 0)).thenRun(() -> {
            for (Player tagger : taggers) {
                holderEntity.addPassenger(tagger);
                tagger.setGlowing(true);
                tagger.teleport(taggerSpawn);
                tagger.updateViewerRule((entity) -> entity.getEntityId() == holderEntity.getEntityId());
                tagger.showTitle(
                        Title.title(
                                Component.text("TAGGER", NamedTextColor.RED, TextDecoration.BOLD),
                                Component.text("Tag all the goons!", NamedTextColor.GRAY),
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(500))
                        )
                );
            }

            getMap().instance().scheduler().submitTask(new Supplier<>() {
                int secondsLeft = 7;

                @Override
                public TaskSchedule get() {
                    if (secondsLeft == 0) { // Release tagger
                        ParkourTagGame.this.gameStage = GameStage.LIVE;

                        sendActionBar(Component.text("The tagger has been released!", NamedTextColor.GOLD));
                        playSound(Sound.sound(SoundEvent.BLOCK_ANVIL_LAND, Sound.Source.MASTER, 0.3f, 2f));

                        beginTimer();

                        for (Player tagger : taggers) {
                            tagger.updateViewerRule(e -> true);
                        }
                        holderEntity.remove();

                        return TaskSchedule.stop();
                    }

                    if (secondsLeft <= 3) {
                        sendActionBar(Component.text("The tagger will be released in " + secondsLeft + "s", NamedTextColor.RED));
                        playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_HAT, Sound.Source.MASTER, 1.5f, 1.5f));
                    }

                    secondsLeft--;
                    return TaskSchedule.tick(ServerFlag.SERVER_TICKS_PER_SECOND);
                }
            });
        });

        for (Player player : this.getPlayers()) {
            player.showBossBar(this.bossBar);

            if (player.getTeam() == null) { // if player is not tagger
                player.teleport(goonSpawn);
                player.setTeam(GOONS_TEAM);
                player.showTitle(
                        Title.title(
                                Component.text("GOON", NamedTextColor.GREEN, TextDecoration.BOLD),
                                Component.text("Run away from the tagger!", NamedTextColor.GRAY),
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(500))
                        )
                );
                goons.add(player);
            }
        }

        ParkourTagAttackListener.registerListener(getEventNode(), this);
        ParkourTagDoubleJumpListener.registerListener(getEventNode(), this);
    }

    private void beginTimer() {
        int playerCount = getPlayers().size();
        int playTime = 400 / Math.max((12 - playerCount), 1); // 100 seconds with 8 players, 40 with 2
        int glowing = 15 + ((playerCount * 15) / 8); // 30 seconds with 8 players, 18 with 2
        int doubleJump = glowing / 2; // 15 seconds with 8 players, 9 with 2

        this.gameTimerTask = getMap().instance().scheduler().submitTask(new Supplier<>() {
            int secondsLeft = playTime;

            @Override
            public TaskSchedule get() {
                if (secondsLeft == 0) {
                    // Tagger has run out of time
                    victory(getGoons());

                    return TaskSchedule.stop();
                }

                if (secondsLeft <= 10) {
                    playSound(Sound.sound(Key.key("minecraft:battle.showdown.count" + (secondsLeft % 2 + 1)), Sound.Source.MASTER, 0.7f, 1f), Sound.Emitter.self());
                    showTitle(
                            Title.title(
                                    Component.empty(),
                                    Component.text(secondsLeft, TextColor.lerp(secondsLeft / 10f, NamedTextColor.RED, NamedTextColor.GREEN)),
                                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
                            )
                    );
                }

                if (secondsLeft == doubleJump) {
                    playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_HAT, Sound.Source.MASTER, 1f, 1.5f), Sound.Emitter.self());
                    showTitle(
                            Title.title(
                                    Component.empty(),
                                    Component.text("The tagger can now double jump!", NamedTextColor.GRAY),
                                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(2))
                            )
                    );

                    for (Player tagger : taggers) {
                        tagger.setAllowFlying(true);
                    }
                }
                if (secondsLeft == glowing) {
                    playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_HAT, Sound.Source.MASTER, 1f, 1.5f), Sound.Emitter.self());
                    showTitle(
                            Title.title(
                                    Component.empty(),
                                    Component.text("Goons are now glowing!", NamedTextColor.GRAY),
                                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(2))
                            )
                    );

                    for (Player goon : goons) {
                        goon.setGlowing(true);
                    }
                }

                bossBar.progress((float) secondsLeft / (float) playTime);

                secondsLeft--;
                return TaskSchedule.tick(ServerFlag.SERVER_TICKS_PER_SECOND);
            }
        });
    }

    public void checkPlayerCounts() {
        if (goons.isEmpty()) {
            victory(taggers);
            return;
        }
        if (taggers.isEmpty()) {
            victory(goons);
            return;
        }

        bossBar.name(
                Component.text()
                        .append(Component.text(goons.size(), TextColor.fromHexString("#cdffc4"), TextDecoration.BOLD))
                        .append(Component.text(goons.size() == 1 ? " goon remaining" : " goons remaining", TextColor.fromHexString("#8fff82")))
                        .build()
        );
    }

    private void victory(Set<Player> winners) {
        this.gameStage = GameStage.VICTORY;

        if (gameTimerTask != null) gameTimerTask.cancel();

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

        for (Player player : getPlayers()) {
            player.hideBossBar(bossBar);

            if (winners.contains(player)) {
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

    public @NotNull Set<Player> getGoons() {
        return goons;
    }

    public @NotNull Set<Player> getTaggers() {
        return taggers;
    }

    public @NotNull GameStage getGameStage() {
        return this.gameStage;
    }



    public MinecraftPhysics getPhysics() {
        return physics;
    }

    @Override
    public void cleanUp() {
        getMap().instance().scheduleNextTick(MinecraftServer.getInstanceManager()::unregisterInstance);

        this.getPhysics().clear();

        MinecraftServer.getBossBarManager().destroyBossBar(this.bossBar);
        if (this.gameTimerTask != null) this.gameTimerTask.cancel();
    }
}
