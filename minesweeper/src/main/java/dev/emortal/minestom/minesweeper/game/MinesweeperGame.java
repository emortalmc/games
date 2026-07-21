package dev.emortal.minestom.minesweeper.game;

import dev.emortal.minestom.core.game.Game;
import dev.emortal.minestom.core.game.config.GameCreationInfo;
import dev.emortal.minestom.core.game.util.GameWinLoseMessages;
import dev.emortal.minestom.minesweeper.board.Board;
import dev.emortal.minestom.minesweeper.map.MapManager;
import dev.emortal.minestom.minesweeper.util.MinesweeperLoseMessages;
import dev.emortal.minestom.minesweeper.view.InteractionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public final class MinesweeperGame extends Game {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinesweeperGame.class);

    private final @NotNull Board board;

    private final TeamAllocator teamAllocator = new TeamAllocator();
    private final PlayerDisconnectHandler disconnectHandler = new PlayerDisconnectHandler(this, this.teamAllocator);
    private final SaveHandler saveHandler;

    public MinesweeperGame(@NotNull GameCreationInfo creationInfo, @NotNull Board board) {
        super(creationInfo, null);

        this.board = board;
        new InteractionManager(this, board);

        this.saveHandler = new SaveHandler(board, creationInfo.playerIds());
        this.saveHandler.startAutosaveTask();

        this.getEventNode().addListener(PlayerBlockBreakEvent.class, event -> event.setCancelled(true));
    }

    @Override
    public void start() {
        // This game is already in a started state when it is created, so we don't do anything on start
    }

    @Override
    public void cleanUp() {
        this.board.getInstance().scheduleNextTick(MinecraftServer.getInstanceManager()::unregisterInstance);
    }

    @Override
    public void onPreJoin(@NotNull Player player) {
        player.setRespawnPoint(MapManager.SPAWN_POSITION);
    }

    @Override
    public void onJoin(@NotNull Player player) {
        player.setAutoViewable(true);
        player.setGameMode(GameMode.CREATIVE);
        player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE).setBaseValue(12);
        this.teamAllocator.allocate(player);

        player.sendMessage(Component.text("Seed: " + this.board.getSeed(), NamedTextColor.AQUA));
    }

    @Override
    public void onLeave(@NotNull Player player) {
        LOGGER.info("{} left", player.getName());
        this.disconnectHandler.onDisconnect(player);
    }

    @Override
    public @NotNull Instance getSpawningInstance(@NotNull Player player) {
        return this.board.getInstance();
    }

    public void win() {
        Title title = Title.title(
                Component.text("VICTORY!", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text(GameWinLoseMessages.randomVictory(), NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(6))
        );
        for (Player player : this.getPlayers()) {
            player.showTitle(title);
        }

        this.board.getInstance().scheduler().buildTask(this::finish).delay(TaskSchedule.seconds(8)).schedule();
    }

    public void lose() {
        Title title = Title.title(
                Component.text("DEFEAT!", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text(MinesweeperLoseMessages.random(), NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(4))
        );
        for (Player player : this.getPlayers()) {
            player.showTitle(title);
        }

        this.board.revealMines();

        this.saveHandler.setEnableSave(false);
        this.saveHandler.delete();

        this.board.getInstance().scheduler().buildTask(this::finish).delay(TaskSchedule.seconds(10)).schedule();
    }

    public void save() {
        LOGGER.info("Saving board");
        this.saveHandler.save();
    }

    public @NotNull Board getBoard() {
        return board;
    }
}
