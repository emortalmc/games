package dev.emortal.minestom.battle.listeners;

import dev.emortal.minestom.battle.game.BattleGame;
import dev.emortal.minestom.battle.game.PlayerTeams;
import io.github.togar2.pvp.events.EntityPreDeathEvent;
import io.github.togar2.pvp.events.FinalDamageEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.minestom.server.ServerFlag;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.HitAnimationPacket;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.potion.TimedPotion;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public final class PvpListener {
    public static final @NotNull Tag<Integer> KILLS_TAG = Tag.Integer("kills");

    private static final Title YOU_DIED_TITLE = Title.title(
            Component.text("YOU DIED", NamedTextColor.RED, TextDecoration.BOLD),
            Component.empty(),
            Title.DEFAULT_TIMES
    );

    private final @NotNull BattleGame game;

    public PvpListener(@NotNull BattleGame game) {
        this.game = game;

        game.getEventNode().addListener(FinalDamageEvent.class, this::onFinalDamage);
        game.getEventNode().addListener(EntityPreDeathEvent.class, this::onPreDeath);
        game.getEventNode().addListener(PlayerTickEvent.class, this::onTick);
    }

    private void onFinalDamage(@NotNull FinalDamageEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        if (player.isInvulnerable()) {
            event.setCancelled(true);
            return;
        }

        Entity source = event.getEntity();

        float animation = source.getPosition().yaw();
        this.sendHitAnimation(player, animation);
    }

    private void sendHitAnimation(@NotNull Entity entity, float animation) {
        this.game.sendGroupedPacket(new HitAnimationPacket(entity.getEntityId(), animation));
    }

    private void onPreDeath(@NotNull EntityPreDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        event.setCancelDeath(true);

        // Get last player that hit
        Damage lastDamage = player.getLastDamageSource();
        // CustomEntityDamage should always be used over EntityDamage due to MinestomPvP
        if (lastDamage != null && lastDamage.getSource() != null && lastDamage.getSource() instanceof Player killer) {
            this.handlePlayerDeath(player, killer);
        } else {
            this.handlePlayerDeath(player, null);
        }

        this.game.checkPlayerCounts();
    }

    private void handlePlayerDeath(@NotNull Player target, @Nullable Player killer) {
        dropItems(target);

        this.game.playSound(Sound.sound(Key.key("battle.death"), Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self());
        this.resetPlayerAfterDeath(target);

        this.sendDeathMessage(target, killer);

        if (killer != null) {
            Integer lastKills = target.getTag(KILLS_TAG);
            if (lastKills == null) {
                lastKills = 0;
            }
            target.setTag(KILLS_TAG, lastKills + 1);

            killer.showTitle(Title.title(
                    Component.empty(),
                    Component.text("☠ " + target.getUsername(), NamedTextColor.RED),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1))
            ));

            target.sendMessage(
                    Component.text()
                            .append(Component.text(killer.getUsername(), NamedTextColor.GOLD))
                            .append(Component.text(" was on ", NamedTextColor.GRAY))
                            .append(Component.text((int) killer.getHealth(), NamedTextColor.RED))
                            .append(Component.text("/20", NamedTextColor.RED))
            );
        }
    }

    private static void dropItems(@NotNull Player player) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        for (ItemStack itemStack : player.getInventory().getItemStacks()) {
            double angle = rand.nextDouble(Math.PI * 2);
            Vec velocity = new Vec(rand.nextDouble(3.0, 6.0), 0, 0)
                    .rotateAroundY(angle);

            ItemEntity itemEntity = new ItemEntity(itemStack);
            itemEntity.setPickupDelay(20, TimeUnit.SERVER_TICK);
            itemEntity.setVelocity(velocity);
            itemEntity.setInstance(player.getInstance(), player.getPosition().add(0, player.getEyeHeight(), 0));
        }
    }

    private void resetPlayerAfterDeath(@NotNull Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        player.setTeam(PlayerTeams.DEAD);
        player.heal();
        player.setFood(20);
        player.setAutoViewable(false);
        player.showTitle(YOU_DIED_TITLE);
        player.getInventory().clear();
    }

    private void sendDeathMessage(@NotNull Player target, @Nullable Player killer) {
        TextComponent.Builder result = Component.text()
                .append(Component.text("☠", NamedTextColor.RED))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(target.getUsername(), NamedTextColor.RED));

        if (killer != null) {
            result.append(Component.text(" was slain by ", NamedTextColor.GRAY));
            result.append(Component.text(killer.getUsername(), NamedTextColor.WHITE));
        } else {
            result.append(Component.text(" died", NamedTextColor.GRAY));
        }

        this.game.sendMessage(result.build());
    }

    private void onTick(@NotNull PlayerTickEvent event) {
        Player player = event.getPlayer();
        if (player.isInvulnerable()) return;

//        if (borderActive && player.gameMode == GameMode.ADVENTURE) {
//            val point = player.position
//            val radius: Double = (e.instance.worldBorder.diameter / 2.0) + 1.5
//            val checkX = point.x() <= e.instance.worldBorder.centerX + radius && point.x() >= e.instance.worldBorder.centerX - radius
//            val checkZ = point.z() <= e.instance.worldBorder.centerZ + radius && point.z() >= e.instance.worldBorder.centerZ - radius
//
//            if (!checkX || !checkZ) {
//                kill(player)
//            }
//        }

        BoundingBox.PointIterator blocksInHitbox = player.getBoundingBox().getBlocks(player.getPosition());

        while (blocksInHitbox.hasNext()) {
            BoundingBox.MutablePoint mutablePoint = blocksInHitbox.next();
            Block block = event.getInstance().getBlock(mutablePoint.blockX(), mutablePoint.blockY(), mutablePoint.blockZ(), Block.Getter.Condition.TYPE);
            this.updateBurning(player, block);
        }
    }

    private void updateBurning(@NotNull Player player, @NotNull Block block) {
        if (block.compare(Block.WATER)) {
            player.setFireTicks(0);
        } else if (block.compare(Block.FIRE)) {
            this.doBurningDamage(player, 6, DamageType.IN_FIRE, 1F);
        } else if (block.compare(Block.LAVA)) {
            this.doBurningDamage(player, 12, DamageType.LAVA, 4F);
        }
    }

    private void doBurningDamage(@NotNull Player player, int durationSeconds, RegistryKey<DamageType> damageType, float damageAmount) {
        player.setFireTicks(durationSeconds * ServerFlag.SERVER_TICKS_PER_SECOND);
        if (player.getAliveTicks() % 10L != 0L) return;

        boolean hasFireResistance = false;
        for (TimedPotion activeEffect : player.getActiveEffects()) {
            if (activeEffect.potion().effect() == PotionEffect.FIRE_RESISTANCE) {
                hasFireResistance = true;
                break;
            }
        }

        if (hasFireResistance) return;
        player.damage(damageType, damageAmount);
    }
}
