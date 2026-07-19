package dev.emortal.minestom.lazertag.game;

import dev.emortal.minestom.core.map.LoadedMap;
import dev.emortal.minestom.lazertag.gun.Gun;
import dev.emortal.minestom.lazertag.util.entity.BetterEntity;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.EntityDamage;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.network.packet.server.play.HitAnimationPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.MathUtils;
import net.minestom.server.utils.position.PositionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public final class DamageHandler {
    private static final Title YOU_DIED_TITLE = Title.title(
            Component.text("YOU DIED", NamedTextColor.RED, TextDecoration.BOLD),
            Component.empty(),
            Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1))
    );

    public static final @NotNull Tag<Long> SPAWN_PROT_TAG = Tag.Long("spawnProt");
    public static final @NotNull Tag<Integer> COMBO_TAG = Tag.Integer("combo");
    public static final @NotNull Tag<Integer> KILLS_TAG = Tag.Integer("kills");
    public static final @NotNull Tag<Integer> DEATHS_TAG = Tag.Integer("deaths");

    private static final DecimalFormat HEALTH_FORMAT = new DecimalFormat("0.##");

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final @NotNull LazerTagGame game;
    private final @NotNull LoadedMap map;

    private boolean firstKill = true;
    private @Nullable Player killLeader = null;

    public DamageHandler(@NotNull LazerTagGame game, @NotNull LoadedMap map) {
        this.game = game;
        this.map = map;
    }

    public void registerListeners() {
        this.game.getEventNode().addListener(PlayerMoveEvent.class, this::onMove);
    }

    private void onMove(@NotNull PlayerMoveEvent event) {
        if (event.getNewPosition().y() < 0) {
            this.kill(event.getPlayer());
        }
    }

    public void damage(@NotNull Player target, @NotNull Player damager, @NotNull Pos sourcePos, float damage) {
        damage = Math.min(20, damage);

        if (target.getGameMode() != GameMode.ADVENTURE) return;

        this.setSpawnProtection(damager, 0L);
        if (this.hasSpawnProtection(target)) {
            this.spawnShieldIndicator(target.getPosition());
            damager.playSound(Sound.sound(SoundEvent.BLOCK_WOOD_BREAK, Sound.Source.MASTER, 1f, 2f), Sound.Emitter.self());
            return;
        }

        Vec direction = sourcePos.sub(target.getPosition()).asVec().normalize();
        float yaw = PositionUtils.getLookYaw(direction.x(), direction.z());

//        game.getInstance().sendGroupedPacket(new DamageEventPacket(target.getEntityId(), 0, damager.getEntityId(), damager.getEntityId(), sourcePos));
        this.game.sendGroupedPacket(new HitAnimationPacket(target.getEntityId(), yaw));

        this.spawnDamageIndicator(target.getPosition(), damage);

        if (this.wouldDie(target, damage)) {
            this.kill(target);
            return;
        }

        damager.playSound(Sound.sound(SoundEvent.BLOCK_BONE_BLOCK_PLACE, Sound.Source.MASTER, 2f, 1f), Sound.Emitter.self());

        target.damage(Damage.fromPlayer(damager, damage));
    }

    private void spawnDamageIndicator(Pos playerPos, float damage) {
        BetterEntity entity = new BetterEntity(EntityType.TEXT_DISPLAY);
        entity.setPhysics(false);
        entity.setAerodynamics(entity.getAerodynamics().withAirResistance(1.0, 1.0));

        float healthPercentage = Math.min(1F, Math.max(0F, damage / 20F));
        TextColor color = TextColor.lerp(healthPercentage, NamedTextColor.DARK_RED, NamedTextColor.GOLD);
        TextColor lighterColor = TextColor.color(
                MathUtils.clamp(color.red() + 100, 0, 255),
                MathUtils.clamp(color.green() + 100, 0, 255),
                MathUtils.clamp(color.blue() + 100, 0, 255)
        );

        TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();

        meta.setNotifyAboutChanges(false);
        meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        meta.setScale(new Vec(2, 2, 0).mul(healthPercentage).add(1, 1, 1));
        meta.setShadow(true);
        meta.setPosRotInterpolationDuration(2);
        meta.setText(Component.text()
                .append(Component.text("❤ ", color))
                .append(Component.text(HEALTH_FORMAT.format(damage), lighterColor))
                .build());
        meta.setNotifyAboutChanges(true);

        // Animated rainbow effect
        if (damage > 18) {
            entity.scheduler()
                    .buildTask(() -> meta.setText(MINI_MESSAGE.deserialize("<rainbow:" + entity.getAliveTicks() / 2 + ">❤ " + HEALTH_FORMAT.format(damage))))
                    .repeat(TaskSchedule.tick(2))
                    .schedule();
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();

        Vec newVelocity = new Vec(
                random.nextDouble(-1, 1),
                random.nextDouble(3 + healthPercentage * 1, 4 + healthPercentage * 1),
                random.nextDouble(-1, 1)
        );
        entity.setVelocity(newVelocity.mul(2)); // TODO: Make sure this is TPS independant

//        Pos newPos = playerPos.add(random.nextDouble(-1.5, 1.5), random.nextDouble(1.7, 2.2), random.nextDouble(-1.5, 1.5));
        Pos newPos = playerPos.add(0, 1.7, 0);

        entity.scheduleRemove(1, ChronoUnit.SECONDS);
        entity.scheduler()
                .buildTask(() -> {
                    if (entity.isOnGround()) entity.remove();
                })
                .repeat(TaskSchedule.nextTick())
                .schedule();

        entity.setInstance(this.map.instance(), newPos);
    }

    private void spawnShieldIndicator(Pos playerPos) {
        BetterEntity entity = new BetterEntity(EntityType.TEXT_DISPLAY);
        entity.setPhysics(false);
        entity.setAerodynamics(entity.getAerodynamics().withAirResistance(1.0, 1.0));

        TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();

        meta.setNotifyAboutChanges(false);
        meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        meta.setScale(new Vec(1.6));
        meta.setShadow(true);
        meta.setPosRotInterpolationDuration(2);
        meta.setText(Component.text("🛡", NamedTextColor.AQUA));
        meta.setNotifyAboutChanges(true);

        ThreadLocalRandom random = ThreadLocalRandom.current();

        Vec newVelocity = new Vec(
                random.nextDouble(-1, 1),
                random.nextDouble(3, 4),
                random.nextDouble(-1, 1)
        );
        entity.setVelocity(newVelocity.mul(2)); // TODO: Make sure this is TPS independant

//        Pos newPos = playerPos.add(random.nextDouble(-1.5, 1.5), random.nextDouble(1.7, 2.2), random.nextDouble(-1.5, 1.5));
        Pos newPos = playerPos.add(0, 1.7, 0);

        entity.scheduleRemove(1, ChronoUnit.SECONDS);
        entity.scheduler()
                .buildTask(() -> {
                    if (entity.isOnGround()) entity.remove();
                })
                .repeat(TaskSchedule.nextTick())
                .schedule();

        entity.setInstance(this.map.instance(), newPos);
    }

    public void kill(Player player) {
        if (player.getGameMode() != GameMode.ADVENTURE) return; // Already dead

        if (player.getLastDamageSource() instanceof EntityDamage damage && damage.getSource() instanceof Player killer) {
            this.game.sendMessage(getDeathMessage(player, killer));

            killer.showTitle(Title.title(
                    Component.empty(),
                    Component.text("☠ " + player.getUsername(), NamedTextColor.RED),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(700))
            ));

            if (this.firstKill) {
                killer.sendActionBar(Component.text("You got the first kill of the game!", NamedTextColor.YELLOW));
                this.game.sendMessage(
                        Component.text()
                                .append(Component.text("☠", NamedTextColor.GOLD))
                                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                                .append(Component.text(killer.getUsername(), NamedTextColor.GOLD))
                                .append(Component.text(" got the ", NamedTextColor.GRAY))
                                .append(Component.text("first kill of the game", NamedTextColor.WHITE))
                                .append(Component.text("!", NamedTextColor.GRAY))
                                .build()
                );
                this.firstKill = false;
            }

            int combo = this.getCombo(killer);
            killer.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 2f, 1f + (combo * 0.1f)), Sound.Emitter.self());
            int kills = this.incrementKills(killer);

            Integer killLeaderKills = 0;
            if (killLeader != null) killLeaderKills = killLeader.getTag(KILLS_TAG);
            if (killLeaderKills == null) killLeaderKills = 0;

            if (kills > killLeaderKills && !killer.equals(this.killLeader)) {
                this.killLeader = killer;

                this.game.sendMessage(Component.text()
                        .append(Component.text("☠", NamedTextColor.GOLD))
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(killer.getUsername(), NamedTextColor.GOLD))
                        .append(Component.text(" is the new ", NamedTextColor.GRAY))
                        .append(Component.text("kill leader", NamedTextColor.WHITE))
                        .append(Component.text("!", NamedTextColor.GRAY))
                        .build());
            }
        } else {
            this.game.sendMessage(this.getDeathMessage(player));
        }

        player.playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_DEATH, Sound.Source.PLAYER, 1, 1), Sound.Emitter.self());

        player.setAutoViewable(false);
        player.setInvulnerable(true);
        player.setGameMode(GameMode.SPECTATOR);
        player.showTitle(YOU_DIED_TITLE);
        player.heal();

        player.scheduler().buildTask(() -> this.startRespawnTimer(player)).delay(TaskSchedule.seconds(2)).schedule();
    }

    public void startRespawnTimer(Player player) {
        // Respawn task
        player.scheduler().submitTask(new Supplier<>() {
            int secondsLeft = 4;

            @Override
            public TaskSchedule get() {
                this.secondsLeft--;

                player.playSound(Sound.sound(SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON, Sound.Source.BLOCK, 1f, 1f), Sound.Emitter.self());
                player.showTitle(Title.title(
                        Component.text(this.secondsLeft, NamedTextColor.GOLD, TextDecoration.BOLD),
                        Component.empty(),
                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(200))
                ));

                if (this.secondsLeft == 0) {
                    DamageHandler.this.respawn(player);
                    return TaskSchedule.stop();
                }

                return TaskSchedule.seconds(1);
            }
        });
    }

    public void respawn(@NotNull Player player) {
        player.teleport(this.getRandomSpawnPoint()).thenRun(() -> this.reset(player));
    }

    private void reset(@NotNull Player player) {
        player.playSound(Sound.sound(SoundEvent.BLOCK_BEACON_ACTIVATE, Sound.Source.MASTER, 1f, 2f), Sound.Emitter.self());
        player.setAutoViewable(true);
        player.setInvulnerable(false);
        player.setVelocity(Vec.ZERO);
        player.setGameMode(GameMode.ADVENTURE);
        player.clearTitle();

        setSpawnProtection(player, 4000); // 4 seconds of spawn protection on respawn

        this.giveRandomGun(player);
    }

    private void giveRandomGun(@NotNull Player player) {
        Gun gun = this.game.getGunManager().getRandomGun();
        player.setItemInMainHand(gun.createItem());
        gun.renderAmmo(player, 1F, false);
    }

    private @NotNull Pos getRandomSpawnPoint() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Pos> spawns = this.map.data().getList("spawns", Pos.class);
        return spawns.get(random.nextInt(spawns.size()));
    }

    private boolean wouldDie(@NotNull Player player, float damage) {
        return player.getHealth() <= damage;
    }

    private void setSpawnProtection(@NotNull Player player, long millis) {
        player.setTag(SPAWN_PROT_TAG, System.currentTimeMillis() + millis);
    }

    private boolean hasSpawnProtection(@NotNull Player player) {
        Long spawnProtectionMillis = player.getTag(SPAWN_PROT_TAG);
        if (spawnProtectionMillis == null) return false;

        return System.currentTimeMillis() < spawnProtectionMillis;
    }

    private int getCombo(@NotNull Player player) {
        return player.getTag(COMBO_TAG);
    }

    private void incrementCombo(@NotNull Player player) {
        int currentCombo = player.getTag(COMBO_TAG);
        player.setTag(COMBO_TAG, currentCombo + 1);
    }

    private int incrementKills(@NotNull Player player) {
        this.incrementCombo(player);
        int currentKills = player.getTag(KILLS_TAG);

        player.setTag(KILLS_TAG, currentKills + 1);
        this.game.getScoreboardHandler().refreshScoreboard();

        if ((currentKills + 1) >= LazerTagGame.KILLS_TO_WIN) {
            this.game.victory();
        }

        return currentKills + 1;
    }

    public @NotNull Component getDeathMessage(@NotNull Player player) {
        return Component.text()
                .append(Component.text("☠", NamedTextColor.RED))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(player.getUsername(), NamedTextColor.RED))
                .append(Component.text(" died", NamedTextColor.GRAY))
                .build();
    }

    public @NotNull Component getDeathMessage(@NotNull Player player, @NotNull Player killer) {
        return Component.text()
                .append(Component.text("☠", NamedTextColor.RED))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(killer.getUsername(), NamedTextColor.WHITE))
                .append(Component.text(" killed ", NamedTextColor.GRAY))
                .append(Component.text(player.getUsername(), NamedTextColor.RED))
//                .append(Component.text(" with ", NamedTextColor.GRAY))
//                .append(Component.text(gunName, NamedTextColor.GOLD))
                .build();
    }
}
