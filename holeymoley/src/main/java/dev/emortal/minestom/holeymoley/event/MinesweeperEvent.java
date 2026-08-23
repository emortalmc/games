package dev.emortal.minestom.holeymoley.event;

import dev.emortal.minestom.holeymoley.game.HoleyMoleyGame;
import io.github.togar2.pvp.entity.explosion.TntEntity;
import net.minestom.server.ServerFlag;
import net.minestom.server.entity.Entity;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.timer.TaskSchedule;

import java.util.concurrent.ThreadLocalRandom;

public class MinesweeperEvent extends Event {

    public MinesweeperEvent() {
        super("Minesweeper");
    }

    @Override
    public void doEvent(HoleyMoleyGame game) {
        EventListener<PlayerBlockBreakEvent> listener = EventListener.of(PlayerBlockBreakEvent.class, this::onBlockBroken);
        game.getEventNode().addListener(listener);

        game.getInstance().scheduler()
                .buildTask(() -> game.getEventNode().removeListener(listener))
                .delay(TaskSchedule.tick(ServerFlag.SERVER_TICKS_PER_SECOND * 10))
                .schedule();
    }

    private void onBlockBroken(PlayerBlockBreakEvent event) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() >= 0.1) return;

        Entity entity = new TntEntity(null);
        entity.setInstance(event.getInstance(), event.getBlockPosition().add(0.5, 0, 0.5));
    }
}
