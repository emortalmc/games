package dev.emortal.minestom.holeymoley.chest;

import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class Item {

    private final @NotNull Material material;
    private final int weight;
    private final @NotNull Consumer<ItemStack.Builder> itemCreate;
    private final @NotNull ItemStack itemStack;

    public Item(@NotNull Material material, int weight, @NotNull Consumer<ItemStack.Builder> itemCreate) {
        this.material = material;
        this.weight = weight;
        this.itemCreate = itemCreate;
        this.itemStack = this.createItemStack();
    }

    public Item(@NotNull Material material, int weight) {
        this(material, weight, builder -> {});
    }

    public Item(@NotNull Material material, int weight, int minAmount, int maxAmount) {
        this(material, weight, builder -> builder.amount(randomAmount(minAmount, maxAmount)));
    }

    private static int randomAmount(int minAmount, int maxAmount) {
        if (minAmount >= maxAmount) return minAmount;
        return ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
    }

    private @NotNull ItemStack createItemStack() {
        ItemStack.Builder builder = ItemStack.builder(this.material);
        this.itemCreate.accept(builder);

        return builder.build();
    }

    public @NotNull ItemStack getItemStack() {
        return this.itemStack;
    }

    public int getWeight() {
        return weight;
    }
}
