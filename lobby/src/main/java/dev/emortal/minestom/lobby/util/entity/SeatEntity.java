package dev.emortal.minestom.lobby.util.entity;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.property.ServerProperties;
import org.jetbrains.annotations.NotNull;

public final class SeatEntity extends BetterEntity {

    private final Runnable onRemove;

    public SeatEntity(@NotNull Runnable onRemove) {
        super(EntityType.TEXT_DISPLAY);

        TextDisplayMeta meta = (TextDisplayMeta) super.entityMeta;
        meta.setBackgroundColor(0);
        meta.setScale(Vec.ZERO);
        super.setTicking(false);

        this.onRemove = onRemove;
    }

    @Override
    public void removePassenger(@NotNull Entity entity) {
        super.removePassenger(entity);

        entity.setVelocity(new Vec(0, ServerProperties.SERVER_TICKS_PER_SECOND.get() * 0.5, 0));

        if (super.getPassengers().isEmpty()) {
            this.onRemove.run();
            super.remove();
        }
    }
}
