package dev.emortal.minestom.lobby.gadget;

import dev.emortal.minestom.core.utils.command.ExtraConditions;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

public abstract class Gadget {

    private final @NotNull String name;
    private final @NotNull Tag<Boolean> tag;
    private final @NotNull ItemStack item;
    private final @NotNull String permissionName;
    private final boolean projectile;

    protected Gadget(@NotNull String name, @NotNull ItemStack item, @NotNull String permissionName, boolean projectile) {
        this.name = name;
        this.tag = Tag.Boolean(name + "gadget");
        this.item = item.withTag(tag, true);
        this.permissionName = permissionName;
        this.projectile = projectile;
    }

    protected Gadget(@NotNull String name, @NotNull ItemStack item, @NotNull String permissionName) {
        this(name, item, permissionName, false);
    }

    protected abstract void onUse(@NotNull Player user, @NotNull Instance instance);

    protected void onCollide(@NotNull Entity entity, @NotNull Instance instance) {
        // do nothing by default - only for projectiles
    }

    public boolean isAllowed(@NotNull Player player) {
        return ExtraConditions.hasPermission(player, "lobby.gadget." + this.permissionName);
    }

    public void give(@NotNull Player player, int slot) {
        player.getInventory().setItemStack(slot, this.item);
    }

    public void registerListeners(@NotNull EventNode<Event> eventNode) {
        eventNode.addListener(PlayerUseItemEvent.class, event -> {
            if (event.getHand() != PlayerHand.MAIN) return;
            if (!event.getItemStack().hasTag(tag)) return;

            event.setCancelled(true);
            this.onUse(event.getPlayer(), event.getInstance());
        });

        if (this.projectile) {
            eventNode.addListener(ProjectileCollideWithBlockEvent.class, event -> {
                this.onCollide(event.getEntity(), event.getInstance());
            });
        }
    }
}
