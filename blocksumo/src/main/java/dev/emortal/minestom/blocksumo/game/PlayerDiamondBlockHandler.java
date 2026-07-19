package dev.emortal.minestom.blocksumo.game;

import dev.emortal.minestom.blocksumo.entity.BetterEntity;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Supplier;

public final class PlayerDiamondBlockHandler {
    private static final int DIAMOND_BLOCK_SECONDS = 20;

    private final @NotNull BlockSumoGame game;

    private @Nullable Player playerOnDiamondBlock = null;
    private @Nullable Task diamondBlockTask = null;

    private @Nullable BetterEntity textEntity = null;
    private @Nullable BetterEntity countdownEntity = null;


    public PlayerDiamondBlockHandler(@NotNull BlockSumoGame game) {
        this.game = game;
    }

    public void registerListeners(@NotNull EventNode<Event> eventNode) {
        eventNode.addListener(PlayerMoveEvent.class, this::onPlayerMove);
    }

    private boolean isValidMove(@NotNull PlayerMoveEvent event) {
        if (this.game.hasEnded()) return false;

        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) return false;

        return event.getNewPosition().sameBlock(BlockSumoGame.CENTER);
    }

    private void onPlayerMove(@NotNull PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isValidMove(event)) {
            this.stopStandingOnBlock(player);
            return;
        }

        if (this.playerOnDiamondBlock != null) return;
        // player is now standing on the diamond block
        this.playerOnDiamondBlock = player;

        this.diamondBlockTask = player.scheduler().submitTask(new DiamondBlockTask(player));
    }

    private void stopStandingOnBlock(@NotNull Player player) {
        if (this.playerOnDiamondBlock != player) return;

        this.stop();
    }

    private void stop() {
        if (this.playerOnDiamondBlock != null) {
            this.playerOnDiamondBlock.setLevel(0);
        }
        this.playerOnDiamondBlock.setGlowing(false);
        this.playerOnDiamondBlock = null;

        if (this.diamondBlockTask != null) {
            this.diamondBlockTask.cancel();
        }

        if (textEntity != null) {
            textEntity.remove();
            textEntity = null;
        }
        if (countdownEntity != null) {
            countdownEntity.remove();
            countdownEntity = null;
        }
    }

    private final class DiamondBlockTask implements Supplier<TaskSchedule> {

        private final @NotNull Player player;
        private final @NotNull TextColor teamColor;
        private final @NotNull BlockSumoGame game = PlayerDiamondBlockHandler.this.game;

        private int secondsLeft = DIAMOND_BLOCK_SECONDS;

        DiamondBlockTask(@NotNull Player player) {
            this.player = player;
            this.teamColor = player.getTag(PlayerTags.TEAM_COLOR).getTextColor();
        }

        @Override
        public TaskSchedule get() {
            if (this.game.hasEnded()) {
                PlayerDiamondBlockHandler.this.stop();
                return TaskSchedule.stop();
            }

            if (this.secondsLeft == 0) {
                PlayerDiamondBlockHandler.this.stop();
                this.game.victory(Set.of(this.player));
                return TaskSchedule.stop();
            }

            this.player.setLevel(this.secondsLeft);
            this.player.playSound(Sound.sound(SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.Source.MASTER, 1f, 1.5f), Sound.Emitter.self());

            if (this.secondsLeft == 10) {
                spawnCountdownTexts();
            }
            if (this.secondsLeft < 10) {
                countdownEntity.editEntityMeta(TextDisplayMeta.class, meta -> {
                    meta.setText(Component.text(this.secondsLeft, NamedTextColor.RED, TextDecoration.BOLD));
                });
            }

            if (this.secondsLeft == 5) {
                player.setGlowing(true);
            }
            if (this.secondsLeft <= 5) {
                this.notifyFinalCountdown();
            }
            if (this.secondsLeft % 5 == 0 && this.secondsLeft != DIAMOND_BLOCK_SECONDS) {
                this.notifyCountdown();
            }

            this.secondsLeft--;
            return TaskSchedule.seconds(1);
        }

        private void spawnCountdownTexts() {
            textEntity = new BetterEntity(EntityType.TEXT_DISPLAY);
            textEntity.editEntityMeta(TextDisplayMeta.class, meta -> {
                meta.setText(Component.text()
                        .append(Component.text(player.getUsername(), NamedTextColor.WHITE))
                        .append(Component.text(" wins in", NamedTextColor.GRAY))
                        .build());
                meta.setScale(new Vec(1.2, 1, 1.2));
                meta.setSeeThrough(true);
                meta.setShadow(true);
                meta.setBackgroundColor(0);
                meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.VERTICAL);
            });
            textEntity.setTicking(false);
            textEntity.setHasCollision(false);
            textEntity.setInstance(game.getInstance(), BlockSumoGame.CENTER.add(0, 5, 0));

            countdownEntity = new BetterEntity(EntityType.TEXT_DISPLAY);
            countdownEntity.editEntityMeta(TextDisplayMeta.class, meta -> {
                meta.setText(Component.text(this.secondsLeft, NamedTextColor.RED, TextDecoration.BOLD));
                meta.setScale(new Vec(5.5));
                meta.setSeeThrough(true);
                meta.setShadow(true);
                meta.setBackgroundColor(0);
                meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.VERTICAL);
            });
            countdownEntity.setTicking(false);
            countdownEntity.setHasCollision(false);
            countdownEntity.setInstance(game.getInstance(), BlockSumoGame.CENTER.add(0, 3.5, 0));
        }

        private void notifyCountdown() {
            this.game.sendMessage(Component.text()
                    .append(Component.text("!", NamedTextColor.RED, TextDecoration.BOLD))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(this.player.getUsername(), this.teamColor, TextDecoration.BOLD))
                    .append(Component.text(" is standing on the diamond block!\n", NamedTextColor.GRAY))
                    .append(Component.text("!", NamedTextColor.RED, TextDecoration.BOLD))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("They win in ", NamedTextColor.GRAY))
                    .append(Component.text(this.secondsLeft, NamedTextColor.RED))
                    .append(Component.text(" seconds!", NamedTextColor.GRAY))
                    .build());

            this.game.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.MASTER, 1f, 1.2f), Sound.Emitter.self());
        }

        private void notifyFinalCountdown() {
            this.game.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 1f, 0.8f), Sound.Emitter.self());
        }
    }
}
