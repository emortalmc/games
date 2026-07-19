package dev.emortal.minestom.blocksumo.game;

import dev.emortal.minestom.blocksumo.powerup.PowerUp;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.WorldEventPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerBlockHandler {
    private static final Point SPAWN = BlockSumoGame.CENTER.sub(0.5, 0, 0.5);

    private static final Direction[] DIRECTIONS = Direction.values();

    // These values are trial-and-error. If they break, blame me. Don't complain, just fix it kekw
    private static final float REACH_TOLERANCE = 1.0f;
    private static final float NON_SNEAKING_RANGE = 4.65f + REACH_TOLERANCE;
    private static final float SNEAKING_RANGE = 4.9f + REACH_TOLERANCE;

    private final @NotNull BlockSumoGame game;

    private final Map<Point, Task> centerBlockBreakTasks = new ConcurrentHashMap<>();

    public PlayerBlockHandler(@NotNull BlockSumoGame game) {
        this.game = game;
    }

    public void registerListeners(@NotNull EventNode<Event> eventNode) {
        eventNode.addListener(PlayerBlockPlaceEvent.class, this::onBlockPlace);
        eventNode.addListener(PlayerBlockBreakEvent.class, this::onBlockBreak);
    }

    private void onBlockPlace(@NotNull PlayerBlockPlaceEvent event) {
        Point pos = event.getBlockPosition();

        // Fixes replacement of non-solid blocks like stairs
        if (event.getInstance().getBlock(pos, Block.Getter.Condition.TYPE).isSolid()) {
            event.setCancelled(true);
            return;
        }

        event.consumeBlock(false);
        Player player = event.getPlayer();

        if (this.nextToBarrier(pos)) {
            event.setCancelled(true);
            // force player downwards when placing next to barriers - avoids clutching literally everything
            player.teleport(player.getPosition().sub(0, 1, 0));
            player.setVelocity(new Vec(0, -20, 0));
            return;
        }

        if (!this.withinLegalRange(player, pos)) {
            event.setCancelled(true);
            return;
        }

        PowerUp heldPowerUp = this.game.getPowerUpManager().getHeldPowerUp(player, event.getHand());
        // Handle powerups before height limit check. It can be aggravating
        if (heldPowerUp != null) {
            this.handlePowerUp(heldPowerUp, event);
            return;
        }

        // Exclude powerup usage from the height limit. It can be aggravating
        if (!this.withinWorldLimits(pos)) {
            event.setCancelled(true);
            return;
        }

        if (this.isAroundCenter(pos)) {
            this.scheduleCenterBlockBreak(pos, event.getBlock());
        }

        // Play block place sound to other players
        for (Player plr : event.getInstance().getPlayers()) {
            if (plr == player) continue; // Sound is already played client side
            plr.playSound(Sound.sound(SoundEvent.BLOCK_WOOL_PLACE, Sound.Source.MASTER, 1f, 0.8f), event.getBlockPosition().add(0.5));
        }
    }

    private void onBlockBreak(@NotNull PlayerBlockBreakEvent event) {
        Task removedTask = this.centerBlockBreakTasks.remove(event.getBlockPosition());
        if (removedTask != null) removedTask.cancel();

        if (!this.isWool(event.getBlock())) event.setCancelled(true);
    }

    private boolean isWool(@NotNull Block block) {
        return block.name().toLowerCase(Locale.ROOT).contains("wool");
    }

    private boolean nextToBarrier(@NotNull Point blockPos) {
        for (Direction direction : DIRECTIONS) {
            Point offsetPos = blockPos.add(direction.normalX(), direction.normalY(), direction.normalZ());
            Block offsetBlock = this.game.getInstance().getBlock(offsetPos, Block.Getter.Condition.TYPE);
            if (offsetBlock == Block.BARRIER) return true;
        }

        return false;
    }

    public boolean withinLegalRange(@NotNull Player player, @NotNull Point blockPos) {
        float range = player.isSneaking() ? SNEAKING_RANGE : NON_SNEAKING_RANGE;

        return blockPos.distanceSquared(player.getPosition().add(0, 1, 0)) <= range * range;
    }

    public boolean withinWorldLimits(@NotNull Point blockPos) {
        return blockPos.y() <= 77 && blockPos.y() >= 45.5;
    }

    public boolean isAroundCenter(@NotNull Point blockPos) {
        return blockPos.distanceSquared(SPAWN) < 3 * 3 && blockPos.blockY() > (SPAWN.blockY() - 1);
    }

    private void scheduleCenterBlockBreak(@NotNull Point blockPos, @NotNull Block block) {
        Instance instance = this.game.getInstance();
        Task task = instance.scheduler()
                .buildTask(() -> {
                    instance.setBlock(blockPos, Block.AIR);
                    this.sendBlockBreakEffect(blockPos, block);
                })
                .delay(TaskSchedule.seconds(5))
                .schedule();
        this.centerBlockBreakTasks.put(blockPos, task);
    }

    private void sendBlockBreakEffect(@NotNull Point blockPos, @NotNull Block block) {
        WorldEventPacket packet = new WorldEventPacket(2001, blockPos, block.stateId(), false);
        this.game.sendGroupedPacket(packet);
    }

    private boolean handlePowerUp(@Nullable PowerUp powerUp, @NotNull PlayerBlockPlaceEvent event) {
        if (powerUp == null) return false;
        if (!powerUp.shouldHandleBlockPlace()) return false;

        event.setCancelled(true);
        powerUp.onBlockPlace(event.getPlayer(), event.getHand(), event.getBlockPosition().add(0.5, 0.1, 0.5));
        return true;
    }
}
