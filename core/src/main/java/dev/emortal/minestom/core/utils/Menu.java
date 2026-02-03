package dev.emortal.minestom.core.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.trait.InventoryEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;

import java.util.function.Consumer;

public abstract class Menu {
    protected final Player player;
    protected final Inventory inventory;
    protected final Int2ObjectMap<EventListener<? extends InventoryEvent>> listeners;

    protected Menu(Player player, InventoryType type, String title) {
        this.player = player;
        this.inventory = new Inventory(type, title);
        this.listeners = new Int2ObjectArrayMap<>();
    }

    public final Player getPlayer() {
        return this.player;
    }

    public final Inventory getInventory() {
        return this.inventory;
    }

    protected final void set(int slot, ItemStack itemStack, Consumer<Click> handler) {
        // clean up old listeners when replacing an item
        for (var listener : this.listeners.int2ObjectEntrySet()) {
            if (listener.getIntKey() != slot) continue;
            this.inventory.eventNode().removeListener(listener.getValue());
        }

        this.listeners.remove(slot);

        // add new listeners
        this.inventory.setItemStack(slot, itemStack);
        var listener = this.eventListener(slot, handler);
        this.inventory.eventNode().addListener(listener);
        this.listeners.put(slot, listener);
    }

    protected final void set(int slot, ItemStack itemStack) {
        this.set(slot, itemStack, _ -> {});
    }

    protected final void add(ItemStack itemStack) {
        this.inventory.addItemStack(itemStack);
    }

    protected final void clear() {
        this.inventory.clear();
        this.listeners.forEach((_, listener) -> this.inventory.eventNode().removeListener(listener));
        this.listeners.clear();
    }

    private EventListener<InventoryPreClickEvent> eventListener(int slot, Consumer<Click> handler) {
        return EventListener.of(InventoryPreClickEvent.class, event -> {
            if (event.getSlot() == slot) {
                event.setCancelled(true);
                handler.accept(event.getClick());
            }
        });
    }
}
