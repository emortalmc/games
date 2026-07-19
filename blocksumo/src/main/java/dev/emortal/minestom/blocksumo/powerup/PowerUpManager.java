package dev.emortal.minestom.blocksumo.powerup;

import dev.emortal.minestom.blocksumo.game.BlockSumoGame;
import dev.emortal.minestom.blocksumo.powerup.item.*;
import dev.emortal.minestom.blocksumo.powerup.item.hook.GrapplingHook;
import dev.emortal.minestom.blocksumo.utils.FireworkUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.color.Color;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.metadata.item.ItemEntityMeta;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.FireworkExplosion;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.utils.WeightedList;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class PowerUpManager {
    private static final Pos FIREWORK_CENTER = BlockSumoGame.CENTER.add(0, 1, 0);

    private final @NotNull PowerUpRegistry registry;
    private final @NotNull BlockSumoGame game;
    private final @NotNull RandomPowerUpHandler randomPowerUpHandler;

    public PowerUpManager(@NotNull BlockSumoGame game) {
        this.registry = new PowerUpRegistry();
        this.game = game;
        this.randomPowerUpHandler = new RandomPowerUpHandler(game, this);
    }

    public void registerListeners(@NotNull EventNode<Event> eventNode) {
        eventNode.addListener(PlayerUseItemEvent.class, this::onItemUse);
        eventNode.addListener(PlayerUseItemOnBlockEvent.class, this::onItemUseOnBlock);

//        eventNode.addListener(ProjectileCollideWithBlockEvent.class, this::onCollideWithBlock);
//        eventNode.addListener(ProjectileCollideWithEntityEvent.class, this::onCollideWithEntity);

        this.randomPowerUpHandler.registerListeners(eventNode);
    }

    private void onItemUse(@NotNull PlayerUseItemEvent event) {
        event.setCancelled(true);

        Player player = event.getPlayer();
        PlayerHand hand = event.getHand();

        PowerUp heldPowerUp = this.getHeldPowerUp(player, hand);
        if (heldPowerUp != null) heldPowerUp.onUse(player, hand);
    }

    private void onItemUseOnBlock(@NotNull PlayerUseItemOnBlockEvent event) {
        Player player = event.getPlayer();
        PlayerHand hand = event.getHand();

        PowerUp heldPowerUp = this.getHeldPowerUp(player, hand);
        if (heldPowerUp != null) heldPowerUp.onUseOnBlock(player, hand);
    }

//    private void onCollideWithBlock(@NotNull ProjectileCollideWithBlockEvent event) {
//        if (!(event.getEntity() instanceof EntityProjectile entity)) return;
//        if (!(entity.getShooter() instanceof Player shooter)) return;
//
//        String powerUpName = this.getPowerUpName(entity);
//        PowerUp powerUp = this.findNamedPowerUp(powerUpName);
//        if (powerUp == null) return;
//
//        powerUp.onCollideWithBlock(shooter, event.getCollisionPosition());
//        if (powerUp.shouldRemoveEntityOnCollision()) entity.remove();
//    }
//
//    private void onCollideWithEntity(@NotNull ProjectileCollideWithEntityEvent event) {
//        if (!(event.getEntity() instanceof EntityProjectile entity)) return;
//        if (!(entity.getShooter() instanceof Player shooter)) return;
//        if (!(event.getTarget() instanceof Player target)) return;
//
//        if (this.game.getSpawnProtectionManager().isProtected(target)) {
//            this.game.getSpawnProtectionManager().notifyProtected(shooter, target);
//            return;
//        }
//
//        if (!target.getTag(PlayerTags.CAN_BE_HIT)) return;
//        target.setTag(PlayerTags.CAN_BE_HIT, false);
//
//        target.scheduler()
//                .buildTask(() -> target.setTag(PlayerTags.CAN_BE_HIT, true))
//                .delay(TaskSchedule.tick(10))
//                .schedule();
//
//        target.damage(Damage.fromPlayer(shooter, 0));
//        HitAnimationPacket hitAnimationPacket = new HitAnimationPacket(target.getEntityId(), entity.getPosition().yaw());
//        this.game.sendGroupedPacket(hitAnimationPacket);
//
//        String powerUpName = this.getPowerUpName(entity);
//        PowerUp powerUp = this.findNamedPowerUp(powerUpName);
//        if (powerUp == null) return;
//
//        powerUp.onCollideWithEntity(entity, shooter, target, event.getCollisionPosition());
//        if (powerUp.shouldRemoveEntityOnCollision()) entity.remove();
//    }

    public void startRandomPowerUpTasks() {
        this.randomPowerUpHandler.startRandomPowerUpTasks();
    }

    public void registerDefaultPowerUps() {
        this.registry.registerPowerUp(new Puncher(this.game));
        this.registry.registerPowerUp(new Slimeball(this.game));
        this.registry.registerPowerUp(new Snowball(this.game));
        this.registry.registerPowerUp(new EnderPearl(this.game));
        this.registry.registerPowerUp(new Tnt(this.game));
        this.registry.registerPowerUp(new AntiGravityTnt(this.game));
        this.registry.registerPowerUp(new KnockbackStick(this.game));
        this.registry.registerPowerUp(new Fireball(this.game));
        this.registry.registerPowerUp(new Switcheroo(this.game));
        this.registry.registerPowerUp(new GrapplingHook(this.game));

        // April Fools
//        this.registry.registerPowerUp(new PortAFort(this.game));
//        this.registry.registerPowerUp(new ExtraLife(this.game));

        // Unobtainable
        this.registry.registerPowerUp(new HotPotato(this.game));
    }

    public @Nullable PowerUp findNamedPowerUp(@NotNull String id) {
        return this.registry.findByName(id);
    }

    public @NotNull PowerUp findRandomPowerUp(@NotNull SpawnLocation spawnLocation) {
        WeightedList<PowerUp> possiblePowerUps = this.registry.findAllBySpawnLocation(spawnLocation);

        return possiblePowerUps.pickOrThrow(ThreadLocalRandom.current());
    }

    public @NotNull PowerUp findRandomPowerUp() {
        return this.registry.findRandom();
    }

    public @Nullable PowerUp getHeldPowerUp(@NotNull Player player, @NotNull PlayerHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        String powerUpId = this.getPowerUpName(heldItem);
        return this.registry.findByName(powerUpId);
    }

    private @NotNull String getPowerUpName(@NotNull TagReadable powerUp) {
        return powerUp.getTag(PowerUp.NAME);
    }

    public void givePowerUp(@NotNull Player player, @NotNull PowerUp powerUp) {
        ItemStack item = powerUp.createItemStack();
        player.getInventory().addItemStack(item);

        player.sendMessage(Component.text("You have been given " + powerUp.getName() + "!", NamedTextColor.GREEN));
        this.playGivenSound(player);
    }

    public void givePowerUpToAll(@NotNull PowerUp powerUp) {
        ItemStack powerUpItem = powerUp.createItemStack();

        for (Player player : this.game.getPlayers()) {
            if (player.getGameMode() != GameMode.SURVIVAL) continue;
            boolean holdingNothing = player.getItemInMainHand().isAir();
            if (holdingNothing) {
                player.getInventory().setItemStack(player.getHeldSlot(), ItemStack.of(Material.STONE), false); // avoid player's hand
            }
            player.getInventory().addItemStack(powerUpItem);
            if (holdingNothing) {
                player.setItemInMainHand(ItemStack.AIR);
            }
        }

        Component message = Component.text()
                .append(powerUp.getItemName())
                .append(Component.text(" has been given to everyone!", NamedTextColor.GRAY))
                .build();
        this.game.sendMessage(message);

        this.playGivenSound(this.game);
    }

    public void spawnPowerUp(@NotNull PowerUp powerUp) {
        ItemStack item = powerUp.createItemStack();
        ItemEntity entity = new ItemEntity(item);

        ItemEntityMeta meta = entity.getEntityMeta();
        meta.setItem(item);
        entity.set(DataComponents.CUSTOM_NAME, item.get(DataComponents.ITEM_NAME));
        meta.setCustomNameVisible(true);

        entity.setNoGravity(true);
        entity.setMergeable(false);
        entity.setPickupDelay(5, TimeUnit.CLIENT_TICK);
        entity.setBoundingBox(0.5, 0.25, 0.5);
        entity.setInstance(this.game.getInstance(), BlockSumoGame.CENTER);

        this.notifySpawned(item);
        this.displaySpawnedFirework();
    }

    public @NotNull Collection<String> getPowerUpIds() {
        return this.registry.getPowerUpNames();
    }

    private void playGivenSound(@NotNull Audience audience) {
        audience.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.PLAYER, 1, 1));
    }

    private void notifySpawned(@NotNull ItemStack powerUp) {
        Component message = Component.text()
                .append(Objects.requireNonNull(powerUp.get(DataComponents.ITEM_NAME)))
                .append(Component.text(" has spawned at the center!", NamedTextColor.GRAY))
                .build();
        this.game.sendMessage(message);
    }

    private void displaySpawnedFirework() {
        FireworkExplosion effect = new FireworkExplosion(
                FireworkExplosion.Shape.SMALL_BALL,
                List.of(new Color(255, 100, 0)),
                List.of(new Color(255, 0, 255)),
                false,
                false
        );
        FireworkUtil.showFirework(this.game.getPlayers(), this.game.getInstance(), FIREWORK_CENTER, List.of(effect));
    }
}
