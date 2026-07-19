package dev.emortal.minestom.lobby.commands;

import dev.emortal.minestom.core.utils.command.ExtraConditions;
import dev.emortal.minestom.lobby.util.entity.BetterEntity;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.ArmorStandMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TrainCommand extends Command {

    public static final Tag<UUID> TRAIN_TAG = Tag.UUID("trainOwner");
    private static final int SPEED = 20;
    private static final int SEPARATION = 2;

    private final List<Entity> entities = new ArrayList<>();

    private final List<Point> points = new ArrayList<>();

    public TrainCommand(@NotNull Instance instance) {
        super("train");

        this.setCondition((sender, cmd) -> Conditions.playerOnly(sender, cmd) && ExtraConditions.hasPermission(sender, "command.lobby.train"));

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) return;

            for (Entity entity : entities) {
                entity.remove();
            }
            entities.clear();
            points.clear();

            int i = 0;
            for (Player p : instance.getPlayers()) {
                BetterEntity entity = new BetterEntity(EntityType.ARMOR_STAND); // not display entity because of weird interpolation issues
                entity.setNoGravity(true);
                entity.setPhysics(false);

                entity.setTag(TRAIN_TAG, player.getUuid());

                ArmorStandMeta meta = (ArmorStandMeta) entity.getEntityMeta();
                meta.setSmall(true);
                meta.setMarker(true);

                entity.setInvisible(true);
                entity.setInstance(instance, p.getPosition());

                entity.addPassenger(p);

                if (p == player) {
                    entity.scheduler().buildTask(() -> {
                        if (entity.getPassengers().isEmpty()) {
                            for (Entity entity1 : entities) {
                                entity1.remove();
                            }
                            entities.clear();
                            points.clear();
                        }

                        queuePoint(entity.getPosition());

                        entity.setVelocity(player.getPosition().direction().mul(SPEED));
                    }).repeat(TaskSchedule.tick(1)).schedule();
                } else {
                    i++;

                    int finalI = i;
                    entity.scheduler().buildTask(() -> {
                        if (finalI * SEPARATION >= points.size()) return;
                        Point pos = points.get(finalI * SEPARATION);
                        entity.teleport(pos.asPos());
                    }).repeat(TaskSchedule.tick(1)).schedule();
                }

                entities.add(entity);
            }
        });
    }

    private void queuePoint(Point point) {
        points.addFirst(point);
        if (points.size() > entities.size() * SEPARATION) {
            points.removeLast();
        }
    }

}
