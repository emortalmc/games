package dev.emortal.minestom.core.raycast;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Instance;

import java.util.List;

public class RaycastUtil {

    public static boolean hasLineOfSight(Entity entity, Entity other) {
        Ray ray = new Ray(entity.getPosition(), entity.getPosition().direction());
        List<Ray.Intersection<Entity>> hit = ray.entities(entity.getInstance().getEntities());
        for (Ray.Intersection<Entity> intersection : hit) {
            if (intersection.object().equals(other)) return true;
        }
        return false;
    }

    public static boolean hasLineOfSight(Instance instance, Point start, Point end) {
        Vec dir = end.sub(start).asVec();
        Ray ray = new Ray(start, dir);
        BlockFinder blocks = ray.findBlocks(instance);
        return !blocks.hasNext();
    }

}
