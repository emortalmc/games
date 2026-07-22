package dev.emortal.minestom.lazertag.ping;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.timer.TaskSchedule;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

public class PingCompensator {

//    private static final int MAX_ROLLBACK_MILLIS = 500;
//    private static final int MAX_ROLLBACK_TICKS = MAX_ROLLBACK_MILLIS / MinecraftServer.TICK_MS;
    private static final int MAX_ROLLBACK_TICKS = 100;
    static {
        System.out.println(MAX_ROLLBACK_TICKS);
    }

    private final Map<UUID, LinkedList<Point>> prevPositionsMap = new HashMap<>();

    public void recordPositions(Entity entity) {
        entity.scheduler().buildTask(() -> {
            addPosition(entity.getUuid(), entity.getPosition());
        }).repeat(TaskSchedule.tick(1)).schedule();
    }

    public Point getPosition(UUID uuid, int tickDelay) {
        LinkedList<Point> prevPositions = prevPositionsMap.computeIfAbsent(uuid, _ -> new LinkedList<>());

        return prevPositions.get(Math.clamp(tickDelay, 0, prevPositions.size() - 1));
    }

    private void addPosition(UUID uuid, Pos pos) {
        LinkedList<Point> prevPositions = prevPositionsMap.computeIfAbsent(uuid, _ -> new LinkedList<>());

        prevPositions.offerFirst(pos);
        if (prevPositions.size() >= MAX_ROLLBACK_TICKS) {
            prevPositions.removeLast();
        }
    }

}
