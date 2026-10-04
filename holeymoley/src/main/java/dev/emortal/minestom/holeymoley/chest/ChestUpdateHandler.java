package dev.emortal.minestom.holeymoley.chest;

import dev.emortal.minestom.holeymoley.game.HoleyMoleyGame;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryCloseEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BlockActionPacket;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

public final class ChestUpdateHandler {
    private static final TaskSchedule CHEST_REFILL_INTERVAL = TaskSchedule.seconds(120);

    private final @NotNull HoleyMoleyGame game;
    private final @NotNull Instance instance;

    private final Map<Point, ChestBlockHandler> chests = new HashMap<>();
    private final Set<Point> unopenedChests = new HashSet<>();
    private final Map<UUID, Point> chestsByPlayer = new HashMap<>();

    public ChestUpdateHandler(@NotNull HoleyMoleyGame game) {
        this.game = game;
        this.instance = game.getInstance();

        this.registerBackgroundTasks();
        game.getEventNode().addListener(PlayerBlockInteractEvent.class, this::onBlockInteract);
        game.getEventNode().addListener(InventoryCloseEvent.class, this::onInventoryClose);
    }

    private void onBlockInteract(@NotNull PlayerBlockInteractEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) return;

        Block block = event.getBlock();
        if (!block.compare(Block.CHEST)) return;

        Point pos = event.getBlockPosition();
        ChestBlockHandler chestHandler = this.getOrCreateChestHandler(pos);

        event.getPlayer().openInventory(chestHandler.getInventory());

        this.unopenedChests.remove(pos);
        this.chestsByPlayer.put(event.getPlayer().getUuid(), pos);

        int playersInside = chestHandler.addPlayerInside();
        this.updatePlayersInsideChest(pos, playersInside);

        if (playersInside == 1) {
            // This is the first player to open the chest
            this.playChestOpenSound(pos);
        }
    }

    private void onInventoryClose(@NotNull InventoryCloseEvent event) {
        Player player = event.getPlayer();

        Point openChestPos = this.chestsByPlayer.remove(player.getUuid());
        if (openChestPos == null) return;

        ChestBlockHandler handler = this.chests.get(openChestPos);
        int playersInside = handler.removePlayerInside();

        this.updatePlayersInsideChest(openChestPos, playersInside);
        if (playersInside == 0) {
            this.playChestCloseSound(openChestPos);
        }
    }

    public @NotNull ChestBlockHandler getOrCreateChestHandler(@NotNull Point pos) {
        ChestBlockHandler handler = this.chests.get(pos);

        if (handler == null) {
            ChestBlockHandler newHandler = new ChestBlockHandler();
            this.chests.put(pos, newHandler);
            handler = newHandler;
        }

        return handler;
    }

    private void updatePlayersInsideChest(@NotNull Point chestPos, int newCount) {
        // The animation of the chest opening and closing is completely controlled by the client
        // all we have to do is just tell it when the players inside count changes
        this.instance.sendGroupedPacket(new BlockActionPacket(chestPos, (byte) 1, (byte) newCount, Block.CHEST));
    }

    private void playChestOpenSound(@NotNull Point pos) {
        this.game.playSound(Sound.sound(SoundEvent.BLOCK_CHEST_OPEN, Sound.Source.BLOCK, 1f, 1f), pos.x(), pos.y(), pos.z());
    }

    private void playChestCloseSound(@NotNull Point pos) {
        this.game.playSound(Sound.sound(SoundEvent.BLOCK_CHEST_CLOSE, Sound.Source.BLOCK, 1f, 1f), pos.x(), pos.y(), pos.z());
    }

    private void registerBackgroundTasks() {
        // Chest periodic refill task
        this.instance.scheduler()
                .buildTask(() -> {
                    if (this.game.hasEnded()) return; // Stop refilling chests once game has been won
                    this.refillChests();
                })
                .delay(CHEST_REFILL_INTERVAL)
                .repeat(CHEST_REFILL_INTERVAL)
                .schedule();

        this.instance.scheduler().buildTask(new DisplayChestRefillParticlesTask()).schedule();
    }

    private void refillChests() {
        this.unopenedChests.clear();

        for (Map.Entry<Point, ChestBlockHandler> chestEntry : this.chests.entrySet()) {
            chestEntry.getValue().refillInventory();
            this.unopenedChests.add(chestEntry.getKey());
        }

        this.animateRefillChestTitle();
    }

    private void animateRefillChestTitle() {
        this.playRefillOpenSound();
        this.instance.scheduler().submitTask(new AnimateChestTask(this.game, this.instance));
    }

    private void playRefillOpenSound() {
        this.game.playSound(Sound.sound(Key.key("battle.refill.open"), Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self());
    }

    private final class DisplayChestRefillParticlesTask implements Runnable {

        private double i = 0.0;

        @Override
        public void run() {
            if (this.i > 2 * Math.PI) {
                this.i = this.i % 2 * Math.PI;
            }

            for (Point unopenedChest : ChestUpdateHandler.this.unopenedChests) {
                ParticlePacket packet = this.createParticlePacket(unopenedChest);
                ChestUpdateHandler.this.game.sendGroupedPacket(packet);
            }
        }

        private @NotNull ParticlePacket createParticlePacket(@NotNull Point pos) {
            return new ParticlePacket(Particle.DUST.withColor(NamedTextColor.YELLOW).withScale(0.75f), pos.x(), pos.y(), pos.z(), 0F, 0F, 0F, 0, 1);
        }
    }

    private static final class AnimateChestTask implements Supplier<TaskSchedule> {
        private static final Component[] FRAMES = new Component[] {Component.text('\uE00E'), Component.text('\uE00F'), Component.text('\uE010')};

        private final @NotNull HoleyMoleyGame game;
        private final @NotNull Instance instance;

        private int frame = 0;

        AnimateChestTask(@NotNull HoleyMoleyGame game, @NotNull Instance instance) {
            this.game = game;
            this.instance = instance;
        }

        @Override
        public @NotNull TaskSchedule get() {
            if (!this.showedAllFrames()) {
                this.showNextFrame();
                this.frame++;
                return TaskSchedule.tick(2);
            }

            this.instance.scheduler().submitTask(new Supplier<>() {
                boolean firstTick = true;
                boolean playedSound = false;

                @Override
                public TaskSchedule get() {
                    if (this.firstTick) {
                        this.firstTick = false;
                        return TaskSchedule.seconds(1);
                    }

                    if (!this.playedSound) {
                        this.playedSound = true;
                        AnimateChestTask.this.playRefillCloseSound();
                    }

                    AnimateChestTask.this.frame--;
                    AnimateChestTask.this.showNextFrame();

                    if (AnimateChestTask.this.frame == 0) {
                        return TaskSchedule.stop();
                    }

                    return TaskSchedule.tick(2);
                }
            });

            return TaskSchedule.stop();
        }

        private boolean showedAllFrames() {
            return this.frame >= FRAMES.length;
        }

        private void showNextFrame() {
            Component nextFrame = FRAMES[this.frame];
            this.game.showTitle(Title.title(
                    nextFrame,
                    Component.empty(),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(200))
            ));
        }

        private void playRefillCloseSound() {
            this.game.playSound(Sound.sound(Key.key("battle.refill.close"), Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self());
        }
    }
}
