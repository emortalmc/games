package dev.emortal.minestom.battle.game;

import com.alibaba.fastjson2.JSONObject;
import dev.emortal.minestom.battle.chest.ChestUpdateHandler;
import dev.emortal.minestom.battle.entity.NoPhysicsEntity;
import dev.emortal.minestom.battle.listeners.ItemListener;
import dev.emortal.minestom.battle.listeners.PvpListener;
import dev.emortal.minestom.core.map.LoadedMap;
import io.github.togar2.pvp.MinestomPvP;
import io.github.togar2.pvp.damage.combat.CombatManager;
import io.github.togar2.pvp.feature.CombatFeatures;
import io.github.togar2.pvp.feature.cooldown.VanillaItemCooldownFeature;
import io.github.togar2.pvp.feature.food.VanillaExhaustionFeature;
import io.github.togar2.pvp.feature.food.VanillaRegenerationFeature;
import io.github.togar2.pvp.feature.tracking.VanillaDeathMessageFeature;
import io.github.togar2.pvp.utils.CombatVersion;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.AreaEffectCloudMeta;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.world.Difficulty;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

final class GameStartHandler {

    private final @NotNull BattleGame game;
    private final @NotNull LoadedMap map;

    private final Set<Entity> freezeEntities = new HashSet<>();

    GameStartHandler(@NotNull BattleGame game, @NotNull LoadedMap map) {
        this.game = game;
        this.map = map;
    }

    @NotNull Task createTimerTask(@NotNull BattleBossBar bossBar) {
        return this.map.instance().scheduler().submitTask(new InitialTimerTask(this.game, bossBar, this.freezeEntities));
    }

    void freezePlayers() {
        game.getEventNode().addChild(
                CombatFeatures.empty()
                        .version(CombatVersion.LEGACY)
                        .difficulty((e) -> Difficulty.NORMAL)
                        .add(CombatFeatures.VANILLA_ARMOR)
                        .add(CombatFeatures.VANILLA_ATTACK)
                        .add(CombatFeatures.VANILLA_CRITICAL)
                        .add(CombatFeatures.VANILLA_EQUIPMENT)
                        .add(CombatFeatures.VANILLA_ATTACK_COOLDOWN)
                        .add(CombatFeatures.VANILLA_ITEM_COOLDOWN)
                        .add(CombatFeatures.VANILLA_DAMAGE)
                        .add(CombatFeatures.VANILLA_EFFECT)
                        .add(CombatFeatures.VANILLA_ENCHANTMENT)
                        .add(CombatFeatures.VANILLA_FALL)
                        .add(CombatFeatures.VANILLA_REGENERATION)
                        .add(CombatFeatures.VANILLA_KNOCKBACK)
                        .add(CombatFeatures.VANILLA_POTION)
                        .add(CombatFeatures.VANILLA_BOW)
                        .add(CombatFeatures.VANILLA_FISHING_ROD)
                        .add(CombatFeatures.VANILLA_MISC_PROJECTILE)
                        .add(CombatFeatures.VANILLA_PROJECTILE_ITEM)
                        .add(CombatFeatures.VANILLA_EXHAUSTION)
                        .add(CombatFeatures.VANILLA_FOOD)
                        .build()
                        .createNode()
        );

        double playerStep = 2 * Math.PI / 8;

        double circleIndex = 0.0;
        for (Player player : this.game.getPlayers()) {
            MinestomPvP.setLegacyAttack(player, true);

            // silly minestompvp
            player.setTag(VanillaItemCooldownFeature.COOLDOWN_END, new HashMap<>());
            player.setTag(VanillaDeathMessageFeature.COMBAT_MANAGER, new CombatManager(player));
            player.setTag(VanillaExhaustionFeature.EXHAUSTION, 0.0f);
            player.setTag(VanillaRegenerationFeature.STARVATION_TICKS, 0);

            player.setFlying(false);
            player.setAllowFlying(false);
            player.setGameMode(GameMode.ADVENTURE);
            player.setInvulnerable(true);

            this.moveToInitialPositionAndFreeze(player, circleIndex);
            circleIndex += playerStep;
        }
    }

    private void moveToInitialPositionAndFreeze(@NotNull Player player, double circleIndex) {
        Pos pos = this.findInitialPosition(circleIndex);
        player.teleport(pos);
        this.freezePlayer(player, pos.add(0, 0.1, 0));
    }

    private @NotNull Pos findInitialPosition(double circleIndex) {
        JSONObject data = this.map.data();
        JSONObject circleCenterJson = data.getJSONObject("circleCenter");
        Pos circleCenter = new Pos(
                circleCenterJson.getDoubleValue("x"),
                circleCenterJson.getDoubleValue("y"),
                circleCenterJson.getDoubleValue("z")
        );
        double circleRadius = data.getDoubleValue("circleRadius");

        double x = Math.sin(circleIndex) * circleRadius;
        double z = Math.cos(circleIndex) * circleRadius;

        return circleCenter.add(x, 0, z).withLookAt(circleCenter);
    }

    private void freezePlayer(@NotNull Player player, @NotNull Pos pos) {
        // Freeze players by using riding entity
        Entity freezeEntity = new NoPhysicsEntity(EntityType.AREA_EFFECT_CLOUD);

        AreaEffectCloudMeta meta = (AreaEffectCloudMeta) freezeEntity.getEntityMeta();
        meta.setRadius(0f);

        freezeEntity.setInstance(this.map.instance(), pos).thenRun(() -> freezeEntity.addPassenger(player));
        this.freezeEntities.add(freezeEntity);
    }

    private static final class InitialTimerTask implements Supplier<TaskSchedule> {
        private static final Title.Times DEFAULT_TIMES = Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(500));

        private final @NotNull BattleGame game;
        private final @NotNull BattleBossBar bossBar;
        private final @NotNull Set<Entity> freezeEntities;

        private int secondsLeft = 10;

        InitialTimerTask(@NotNull BattleGame game, @NotNull BattleBossBar bossBar, @NotNull Set<Entity> freezeEntities) {
            this.game = game;
            this.bossBar = bossBar;
            this.freezeEntities = freezeEntities;
        }

        @Override
        public TaskSchedule get() {
            if (this.secondsLeft == 0) {
                this.startGame();
                return TaskSchedule.stop();
            }

            this.game.showTitle(Title.title(Component.empty(), Component.text(this.secondsLeft), DEFAULT_TIMES));
            if (this.secondsLeft <= 5) {
                this.game.playSound(Sound.sound(Key.key("battle.countdown.begin"), Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self());
            }

            this.secondsLeft--;
            return TaskSchedule.seconds(1);
        }

        private void startGame() {
            this.notifyGameStarted();
            this.unfreezeAll();

            this.registerEvents();
            this.bossBar.show(this.game);

            this.game.checkPlayerCounts();
            this.game.beginTimer();
        }

        private void notifyGameStarted() {
            this.game.showTitle(Title.title(Component.empty(), Component.text("Round start!"), DEFAULT_TIMES));
            this.game.playSound(Sound.sound(Key.key("battle.countdown.beginover"), Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self());
        }

        private void unfreezeAll() {
            for (Entity freezeEntity : this.freezeEntities) {
                freezeEntity.remove();
            }
            this.freezeEntities.clear();
        }

        private void registerEvents() {
            new PvpListener(this.game);
            new ItemListener(this.game);
            new ChestUpdateHandler(this.game);
        }
    }
}
