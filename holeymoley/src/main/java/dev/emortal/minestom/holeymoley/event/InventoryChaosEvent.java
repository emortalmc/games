package dev.emortal.minestom.holeymoley.event;

import dev.emortal.minestom.holeymoley.game.HoleyMoleyGame;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InventoryChaosEvent extends Event {

    public InventoryChaosEvent() {
        super("Inventory Chaos");
    }

    @Override
    public void doEvent(HoleyMoleyGame game) {
        for (Player player : game.getAlivePlayers()) {
            List<ItemStack> items = new ArrayList<>(List.of(player.getInventory().getItemStacks()));
            Collections.shuffle(items);

            int i = 0;
            for (ItemStack item : items) {
                player.getInventory().setItemStack(i++, item);
            }
        }
    }
}
