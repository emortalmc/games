package dev.emortal.minestom.holeymoley.chest;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class ChestBlockHandler implements BlockHandler {

    private final Inventory inventory = new Inventory(InventoryType.CHEST_3_ROW, "");
    private final AtomicInteger playersInside = new AtomicInteger(0);

    public ChestBlockHandler() {
        super();
        this.refillInventory();
    }

    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    public int addPlayerInside() {
        return this.playersInside.incrementAndGet();
    }

    public int removePlayerInside() {
        return this.playersInside.decrementAndGet();
    }

    @Override
    public @NotNull Key getKey() {
        return Block.CHEST.key();
    }

    public void refillInventory() {
        this.inventory.clear();
        for (int i = 0; i < 7; i++) {
            addRandomly(this.inventory, Items.random());
        }
    }

    private static void addRandomly(@NotNull Inventory inventory, @NotNull ItemStack itemStack) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        while (true) {
            int randomSlot = random.nextInt(inventory.getSize());
            if (inventory.getItemStack(randomSlot) == ItemStack.AIR) {
                inventory.setItemStack(randomSlot, itemStack);
                break;
            }
        }
    }
}
