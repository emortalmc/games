package dev.emortal.minestom.lazertag.gun;

import dev.emortal.minestom.core.raycast.BlockFinder;
import dev.emortal.minestom.core.raycast.Ray;
import dev.emortal.minestom.lazertag.command.PingCompensationCommand;
import dev.emortal.minestom.lazertag.game.LazerTagGame;
import dev.emortal.minestom.lazertag.ping.PingCompensator;
import dev.emortal.minestom.lazertag.util.DisplayEntityUtil;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.WeightedList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public abstract class Gun {
    private static final Component RELOADING_COMPONENT = Component.text("RELOADING ", NamedTextColor.RED);

    public static final @NotNull Tag<@NotNull String> NAME_TAG = Tag.String("name");
    public static final @NotNull Tag<@NotNull Integer> AMMO_TAG = Tag.Integer("ammo");
    public static final @NotNull Tag<@NotNull Boolean> RELOADING_TAG = Tag.Boolean("reloading");
    public static final @NotNull Tag<@NotNull Long> COOLDOWN_TAG = Tag.Long("cooldown");

    protected final @NotNull LazerTagGame game;
    private final @NotNull String name;
    private final @NotNull GunItemInfo itemInfo;

    protected Gun(@NotNull LazerTagGame game, @NotNull String name, @NotNull GunItemInfo itemInfo) {
        this.game = game;
        this.name = name;
        this.itemInfo = itemInfo;
    }

    public void shoot(@NotNull Player shooter, int ammo) {
        int ping = game.getPlayerPing(shooter);

        for (int i = 0; i < this.itemInfo.bullets(); i++) {
            Vec shootDir = spread(shooter.getPosition().direction(), this.itemInfo.spread());
            Pos eyePos = shooter.getPosition().add(0, shooter.getEyeHeight(), 0);

            Ray ray = new Ray(eyePos, shootDir.mul(this.itemInfo.distance()));
            BlockFinder blocks = ray.findBlocks(shooter.getInstance());
            Ray.Intersection<Block> closestBlockIntersection = blocks.nextClosest();
            Point blockHitPos = closestBlockIntersection == null ? null : closestBlockIntersection.point();
            double blockHitDist = blockHitPos == null ? Double.MAX_VALUE : blockHitPos.distanceSquared(eyePos);

            List<Ray.Intersection<Entity>> hit = new ArrayList<>();

            for (Player player : shooter.getInstance().getPlayers()) {
                if (player == shooter) continue;
                if (player.getGameMode() != GameMode.ADVENTURE) continue;

                PingCompensator pingCompensator = game.getPingCompensator();
                Pos historicalNpcPos = pingCompensator.getPosition(player.getUuid(), ping + PingCompensationCommand.TICKS).asPos();
                Ray.Intersection<Entity> cast = ray.cast(player, historicalNpcPos);
                hit.add(cast);
            }

            // TODO: hit block animation
            boolean anyHit = false;

            for (Ray.Intersection<Entity> intersection : hit) {
                if (intersection == null) continue;
                Entity entity = intersection.object();
                if (!(entity instanceof Player player)) continue;

                Point hitPoint = intersection.point();
                if (hitPoint.distanceSquared(eyePos) > blockHitDist) continue;

                anyHit = true;

                this.game.getDamageHandler().damage(player, shooter, shooter.getPosition(), this.itemInfo.damage());

                renderBulletTrail(eyePos, shootDir, hitPoint);
            }

            if (!anyHit) {
                Point hitPoint = eyePos.add(shootDir.mul(this.itemInfo.distance()));
                renderBulletTrail(eyePos, shootDir, hitPoint);
                return;
            }
        }
    }

    private void renderBulletTrail(Pos eyePos, Vec direction, Point hitPoint) {
        List<Entity> entities = DisplayEntityUtil.drawLine(this.game.getInstance(), eyePos.add(direction.mul(2.0)), hitPoint, new java.awt.Color(255, 255, 0, 100).getRGB(), 0, 0.3);
        this.game.getInstance().scheduler().buildTask(() -> {
            for (Entity entity : entities) {
                entity.remove();
            }
        }).delay(TaskSchedule.tick(7)).schedule();
    }

    public void afterShoot(Player shooter, int ammo) {
        float ammoPercentage = ammo / (float) this.itemInfo.ammo();
        this.renderAmmo(shooter, ammoPercentage, false);

        // Decrease ammo
        shooter.setItemInMainHand(
                shooter.getItemInMainHand()
                    .withTag(AMMO_TAG, ammo)
                    .withTag(COOLDOWN_TAG, System.currentTimeMillis() + this.itemInfo.shootDelay())
        );

        // If ran out of ammo - reload!
        if (ammo == 0) {
            this.reload(shooter);
        }
    }

    public void reload(@NotNull Player player) {
        ItemStack item = player.getItemInMainHand();
        player.setItemInMainHand(
                item
                        .withTag(RELOADING_TAG, true)
                        .withTag(AMMO_TAG, 0)
        );

        player.playSound(Sound.sound(SoundEvent.BLOCK_ANVIL_LAND, Sound.Source.PLAYER, 0.7f, 2f));
        player.scheduler().submitTask(new Supplier<>() {
            final long startingReloadTicks = Gun.this.itemInfo.reloadTime() / MinecraftServer.TICK_MS;

            long reloadTicks = this.startingReloadTicks;
            int lastAmmo = -1;
//            long currentAmmo = 0;

            @Override
            public TaskSchedule get() {
                this.reloadTicks--;

                if (this.reloadTicks == 0) {
                    // Fully reloaded!
                    Gun.this.playReloadSound(player);

                    player.setItemInMainHand(
                            item
                                    .withTag(RELOADING_TAG, false)
                                    .withTag(AMMO_TAG, Gun.this.itemInfo.ammo())
                    );
                    Gun.this.renderAmmo(player, 1f, false);

                    return TaskSchedule.stop();
                }

                float percentage = 1f - (this.reloadTicks / (float) this.startingReloadTicks);
                int ammo = (int) (Gun.this.itemInfo.ammo() * percentage);

                if (ammo != this.lastAmmo) {
                    this.lastAmmo = ammo;
                    player.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.MASTER, 0.2f, 1f), Sound.Emitter.self());
                }

                Gun.this.renderAmmo(player, percentage, true);
                return TaskSchedule.tick(1);
            }
        });
    }

    protected void playReloadSound(@NotNull Player player) {
        player.playSound(Sound.sound(SoundEvent.ENTITY_IRON_GOLEM_ATTACK, Sound.Source.PLAYER, 1f, 1f));
        player.scheduler()
                .buildTask(() -> player.playSound(Sound.sound(SoundEvent.ENTITY_IRON_GOLEM_ATTACK, Sound.Source.PLAYER, 1f, 1f)))
                .delay(TaskSchedule.tick(3))
                .schedule();
    }

    public void renderAmmo(@NotNull Player player, float percentage, boolean reloading) {
        TextComponent.Builder component = Component.text();
        if (reloading) {
            component.append(RELOADING_COMPONENT);
        }

        component.append(createProgressBar(percentage, 40, "|", reloading ? NamedTextColor.RED : NamedTextColor.GOLD, NamedTextColor.DARK_GRAY));

        int ammo = (int) (this.itemInfo.ammo() * percentage);

        component.append(Component.space());
        component.append(Component.text(String.format("%0" + String.valueOf(this.itemInfo.ammo()).length() + "d", ammo), NamedTextColor.WHITE));

        player.sendActionBar(component.build());
    }

    public static @NotNull Vec spread(@NotNull Vec vec, double amount) {
        if (amount == 0.0) return vec;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        return vec
                .rotateAroundX(random.nextDouble(-amount, amount))
                .rotateAroundY(random.nextDouble(-amount, amount))
                .rotateAroundZ(random.nextDouble(-amount, amount));
    }

    private static @NotNull Component createProgressBar(float percentage, int charLength, @NotNull String character, @NotNull RGBLike completeColor,
                                                        @NotNull RGBLike incompleteColor) {
        int completeCharacters = (int) Math.ceil(percentage * charLength);
        int incompleteCharacters = charLength - completeCharacters;

        return Component.text()
                .append(Component.text(character.repeat(completeCharacters), TextColor.color(completeColor)))
                .append(Component.text(character.repeat(incompleteCharacters), TextColor.color(incompleteColor)))
                .build();
    }

    protected ItemStack getCustomItem(String modelId) {
        return ItemStack.builder(Material.PHANTOM_MEMBRANE)
                .set(DataComponents.ITEM_NAME, Component.text(this.name))
                .set(DataComponents.LORE, List.of(this.itemInfo.rarity().getName()))
                .itemModel(modelId)
                .set(AMMO_TAG, this.itemInfo.ammo())
                .set(NAME_TAG, this.name)
                .set(COOLDOWN_TAG, 0L)
                .build();
    }

    public @NotNull ItemStack createItem() {
        return ItemStack.builder(this.itemInfo.material())
                .set(DataComponents.ITEM_NAME, Component.text(this.name))
                .set(DataComponents.LORE, List.of(this.itemInfo.rarity().getName()))
                .set(AMMO_TAG, this.itemInfo.ammo())
                .set(NAME_TAG, this.name)
                .set(COOLDOWN_TAG, 0L)
                .build();
    }

    public @NotNull String getName() {
        return this.name;
    }

    public @NotNull GunItemInfo getItemInfo() {
        return this.itemInfo;
    }

    public @NotNull WeightedList.Entry<Gun> getWeightedEntry() {
        return new WeightedList.Entry<>(this, getItemInfo().rarity().getWeight());
    }

}
