package dev.emortal.minestom.holeymoley.chest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.component.PotionContents;
import net.minestom.server.item.enchant.Enchantment;
import net.minestom.server.potion.CustomPotionEffect;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.potion.PotionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class Items {

    private static final int COMMON = 40;
    private static final int LESS_COMMON = 35;
    private static final int UNCOMMON = 30;
    private static final int RARE = 25;
    private static final int VERY_RARE = 15;
    private static final int LEGENDARY = 1;

    private static final @NotNull List<Item> ALL = List.of(
            // Consumables
            new Item(Material.APPLE, COMMON),
            new Item(Material.APPLE, COMMON),
            new Item(Material.COOKED_BEEF, COMMON),
            new Item(Material.COOKED_PORKCHOP, COMMON),
            new Item(Material.COOKED_PORKCHOP, COMMON),
            new Item(Material.GOLDEN_APPLE, VERY_RARE),

            // Blocks
            new Item(Material.TNT, RARE, 1, 3),
            new Item(Material.COBWEB, RARE, 2, 5),

            // Swords
            new Item(Material.STONE_SWORD, UNCOMMON),
            new Item(Material.IRON_SWORD, VERY_RARE),
            new Item(Material.DIAMOND_SWORD, LEGENDARY),

            // Axes
            new Item(Material.WOODEN_AXE, RARE),
            new Item(Material.STONE_AXE, LEGENDARY),

            // Pickaxes
            new Item(Material.WOODEN_PICKAXE, COMMON),
            new Item(Material.GOLDEN_PICKAXE, LESS_COMMON),
            new Item(Material.STONE_PICKAXE, UNCOMMON),
            new Item(Material.IRON_PICKAXE, RARE),
            new Item(Material.DIAMOND_PICKAXE, VERY_RARE),

            // Misc
            new Item(Material.WOODEN_HOE, RARE, (it) -> it.set(DataComponents.ENCHANTMENTS, new EnchantmentList(Map.of(Enchantment.FIRE_ASPECT, 1)))),
            new Item(Material.DIAMOND_HOE, LEGENDARY),

            // Potions
            new Item(Material.POTION, RARE, (it) -> it.set(DataComponents.POTION_CONTENTS, new PotionContents(PotionType.STRONG_HEALING))),
            new Item(Material.POTION, RARE, (it) -> it.set(DataComponents.POTION_CONTENTS, new PotionContents(PotionType.FIRE_RESISTANCE))),
            new Item(Material.POTION, RARE, potionHelper(PotionEffect.INVISIBILITY, 20)),
            new Item(Material.POTION, VERY_RARE, potionHelper(PotionEffect.STRENGTH, 20)),
            new Item(Material.SPLASH_POTION, RARE, (it) -> it.set(DataComponents.POTION_CONTENTS, new PotionContents(PotionType.REGENERATION))),
            new Item(Material.SPLASH_POTION, RARE, (it) -> it.set(DataComponents.POTION_CONTENTS, new PotionContents(PotionType.STRONG_HARMING))),
            new Item(Material.SPLASH_POTION, RARE, potionHelper(PotionEffect.POISON, 16)),
            new Item(Material.SPLASH_POTION, RARE, potionHelper(PotionEffect.SLOWNESS, 40)),
            new Item(Material.SPLASH_POTION, RARE, potionHelper(PotionEffect.WEAKNESS, 45)),

            // Armour
            // Helmets
            new Item(Material.LEATHER_HELMET, COMMON),
            new Item(Material.GOLDEN_HELMET, UNCOMMON),
            new Item(Material.IRON_HELMET, VERY_RARE),
            new Item(Material.DIAMOND_HELMET, LEGENDARY),

            // Chestplates
            new Item(Material.LEATHER_CHESTPLATE, COMMON),
            new Item(Material.CHAINMAIL_CHESTPLATE, UNCOMMON),
            new Item(Material.IRON_CHESTPLATE, VERY_RARE),
            new Item(Material.DIAMOND_CHESTPLATE, LEGENDARY),

            // Leggings
            new Item(Material.LEATHER_LEGGINGS, COMMON),
            new Item(Material.CHAINMAIL_LEGGINGS, UNCOMMON),
            new Item(Material.IRON_LEGGINGS, VERY_RARE),
            new Item(Material.DIAMOND_LEGGINGS, LEGENDARY),

            // Boots
            new Item(Material.LEATHER_BOOTS, COMMON),
            new Item(Material.CHAINMAIL_BOOTS, UNCOMMON),
            new Item(Material.IRON_BOOTS, VERY_RARE),
            new Item(Material.DIAMOND_BOOTS, LEGENDARY)
    );

    private Items() {
    }

    public static @NotNull ItemStack random() {
        int totalWeight = 0;
        for (Item item : ALL) {
            totalWeight += item.getWeight();
        }

        int currentIndex = 0;
        int randomIndex = ThreadLocalRandom.current().nextInt(totalWeight + 1);

        while (currentIndex < ALL.size() - 1) {
            randomIndex -= ALL.get(currentIndex).getWeight();
            if (randomIndex <= 0.0) break;
            ++currentIndex;
        }

        return ALL.get(currentIndex).getItemStack();
    }

    private static @NotNull Consumer<ItemStack.Builder> potionHelper(@NotNull PotionEffect potionEffect, int secondsDuration) {
        String potionName = titleCase(potionEffect.key().value().replace('_', ' '));
        Component potionDisplayName = Component.text("Potion of " + potionName, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false);

        return builder -> builder
                .set(DataComponents.POTION_CONTENTS, new PotionContents(List.of(new CustomPotionEffect(potionEffect, (byte) 0, secondsDuration * 20, false, true, true))))
                .customName(potionDisplayName);
    }

    private static @NotNull String titleCase(@NotNull String input) { // fire resistance -> Fire Resistance
        char[] chars = input.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            chars[0] = Character.toUpperCase(chars[0]);
            if (chars[i] == ' ') {
                chars[i + 1] = Character.toUpperCase(chars[i + 1]);
            }
        }

        return String.valueOf(chars);
    }
}
