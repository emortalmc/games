package dev.emortal.minestom.minesweeper.view;

import dev.emortal.minestom.minesweeper.board.Board;
import dev.emortal.minestom.minesweeper.game.MinesweeperGame;
import dev.emortal.minestom.minesweeper.game.PlayerTags;
import dev.emortal.minestom.minesweeper.map.MinesweeperMapManager;
import dev.emortal.minestom.minesweeper.util.Flag;
import dev.emortal.minestom.minesweeper.util.Vec2;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.inventory.InventoryItemChangeEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class InteractionManager {

    private final @NotNull MinesweeperGame game;
    private final @NotNull Board board;
    private final @NotNull ActionBar actionBar;

    private int solvedChunks;

    private boolean firstClick;
    private boolean finished;

    public InteractionManager(@NotNull MinesweeperGame game, @NotNull Board board) {
        this.game = game;
        this.board = board;
        this.actionBar = new ActionBar(board);

        this.firstClick = this.board.getStartingTicks() == 0; // only give first click protection if board is brand new

        game.getEventNode().addListener(PlayerBlockBreakEvent.class, this::onBreak);
        game.getEventNode().addListener(PlayerBlockInteractEvent.class, this::onClick);
        game.getEventNode().addListener(PlayerBlockPlaceEvent.class, event -> event.setCancelled(true));
        game.getEventNode().addListener(InventoryItemChangeEvent.class, this::onItemChange);
    }

    private void onBreak(@NotNull PlayerBlockBreakEvent event) {
        event.setCancelled(true); // We never actually want to break the block
        if (this.finished) return;

        Player player = event.getPlayer();

        Point pos = event.getBlockPosition();
        int x = pos.blockX();
        int y = pos.blockY();
        int z = pos.blockZ();

        if (this.isOutsideBoard(x, y, z)) return;
        if (this.board.isFlagged(x, z)) return;
        if (this.board.isRevealed(x, z)) return;
        if (!player.getItemInMainHand().isAir()) return;

        if (this.board.isMine(x, z)) {
            if (this.firstClick) {
                // make sure first click is not bomb
                this.board.getInstance().setBlock(x, MinesweeperMapManager.FLOOR_HEIGHT, z, this.board.getTheme().nothing());
            } else {
                this.damage(player, x, z);
                return;
            }
        }

        this.firstClick = false;

        Chunk chunk = player.getInstance().getChunkAt(pos);
        if (chunk == null) return;

        this.board.addClick(new Vec2(x, z), chunk);
        Set<Chunk> affectedChunks = this.board.revealAround(x, z);

        for (Chunk affectedChunk : affectedChunks) {
            if (affectedChunk == null) continue;

            affectedChunk.sendChunk();

            if (this.board.isSolved(affectedChunk)) {
                this.solveChunk(affectedChunk, player);
            }
        }

        if (!this.board.isInfinite() && this.board.getUnrevealedCount() <= 0) {
            this.win(player);
            return;
        }

        this.actionBar.update();
    }

    private void onClick(@NotNull PlayerBlockInteractEvent event) {
        // We place and remove the carpet ourselves
        // This makes the logic significantly easier, rather than having to cancel
        // everywhere separately, and risking missing something
        event.setCancelled(true);
        if (this.finished) return;

        Player player = event.getPlayer();
        Point pos = event.getBlockPosition();
        Chunk chunk = event.getInstance().getChunkAt(pos);
        if (chunk == null) return;

        int x = pos.blockX();
        int y = pos.blockY();
        int z = pos.blockZ();

        if (event.getHand() != PlayerHand.MAIN) return;
        if (!player.getItemInMainHand().isAir()) {
            // This avoids players being able to place anything other than the carpets that
            // get placed when they click with an empty hand
            return;
        }

        // If the block is already carpet (a flag), we want to remove the flag
        if (this.board.isFlagged(x, z)) {
            if ((this.board.getFlag(x, z) != Block.BLACK_CARPET)
                    && !this.board.getSolvedChunks().contains(new Vec2(chunk.getChunkX(), chunk.getChunkZ()))) {
                // But only if it is not from a lost life or a completed chunk
                player.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.MASTER, 0.6F, 1.8F));
                this.actionBar.decrementFlags();

                this.board.removeFlag(new Flag(new Vec2(x, z), player.getTag(PlayerTags.COLOR)), chunk);
            }

            return;
        }

        if (this.isInvalidFlag(x, y, z)) return;

        player.playSound(Sound.sound(SoundEvent.ENTITY_ENDER_DRAGON_FLAP, Sound.Source.MASTER, 0.6F, 2F));
        this.actionBar.incrementFlags();

        this.board.addFlagIfMissing(new Flag(new Vec2(x, z), player.getTag(PlayerTags.COLOR)), chunk);
    }

    private void onItemChange(@NotNull InventoryItemChangeEvent event) {
        // This is used to stop players from being able to take items out of the
        // creative inventory
        event.getInventory().setItemStack(event.getSlot(), ItemStack.AIR);
    }

    private void win(@NotNull Player player) {
        this.playWinSounds(player);
        this.game.win();
        this.finished = true;
    }

    private void solveChunk(Chunk chunk, Player player) {
        this.board.revealSolved(chunk, player.getTag(PlayerTags.COLOR));
        this.board.addSolvedChunk(chunk);
        this.playSolvedChunkSound(this.board.getInstance());

        this.solvedChunks += 1;

        if (this.solvedChunks % 3 == 0) {
            this.actionBar.incrementLives();
        }
    }

    private void damage(@NotNull Player player, int x, int z) {
        int lives = this.actionBar.decrementLives();
        Chunk chunk = player.getInstance().getChunkAt(new Pos(x, MinesweeperMapManager.FLOOR_HEIGHT, z));

        this.board.addFlag(new Flag(new Vec2(x, z), player.getTag(PlayerTags.COLOR)), Block.BLACK_CARPET, chunk);

        Component lossMessage = Component.text().append(Component.text(player.getUsername(), NamedTextColor.RED))
                .append(Component.text(" clicked a bomb :\\", NamedTextColor.GRAY)).build();

        for (Player gamePlayer : this.game.getPlayers()) {
            this.playMineSound(gamePlayer);
            gamePlayer.sendMessage(lossMessage);
        }

        this.playMineSound(player);

        if (lives <= 0) {
            this.loseGame(player);
        }
    }

    private void loseGame(@NotNull Player player) {
        this.game.lose();
        this.finished = true;
    }

    private boolean isInvalidFlag(int x, int y, int z) {
        return this.isOutsideBoard(x, y, z) || // Out of bounds
                this.board.isFlagged(x, z) || // Somehow clicked a block with carpet on top, cannot flag
                this.board.isRevealed(x, z); // Is revealed
    }

    private boolean isOutsideBoard(int x, int y, int z) {
        if (this.board.isOutOfBounds(x, z)) return true;
        return y != MinesweeperMapManager.FLOOR_HEIGHT;
    }

    private void playRevealSound(@NotNull Audience audience) {
        audience.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.MASTER, 0.5F, 0.7F),
                Sound.Emitter.self());
    }

    private void playSolvedChunkSound(@NotNull Audience audience) {
        audience.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 0.5F, 1F),
                Sound.Emitter.self());
    }

    private void playMineSound(@NotNull Audience audience) {
        audience.playSound(Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.MASTER, 4F, 0.84F),
                Sound.Emitter.self());
    }

    private void playWinSounds(@NotNull Audience audience) {
        audience.playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_CELEBRATE, Sound.Source.MASTER, 1F, 1F),
                Sound.Emitter.self());
        audience.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1F, 1F),
                Sound.Emitter.self());
    }
}
