package dev.emortal.minestom.blocksumo.event.events;

import dev.emortal.minestom.blocksumo.game.BlockSumoGame;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.ServerFlag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EquipmentSlotGroup;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.AttributeList;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.enchant.Enchantment;
import net.minestom.server.property.ServerProperties;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class InstaBreakEvent implements BlockSumoEvent {
    private static final Component START_MESSAGE = MiniMessage.miniMessage()
            .deserialize("<red>Uh oh... <gray>your shears are suddenly more efficient, <yellow>blocks now break instantly");

    private static final AttributeModifier BREAK_SPEED_MODIFIER = new AttributeModifier(Key.key("instabreak"), 30, AttributeOperation.ADD_MULTIPLIED_TOTAL);

    private static final ItemStack ENCHANTED_SHEARS = ItemStack.builder(Material.SHEARS)
            .set(DataComponents.ENCHANTMENTS, new EnchantmentList(Map.of(Enchantment.EFFICIENCY, 4)))
            .set(DataComponents.ATTRIBUTE_MODIFIERS, new AttributeList(new AttributeList.Modifier(Attribute.BLOCK_BREAK_SPEED, BREAK_SPEED_MODIFIER, EquipmentSlotGroup.HAND)))
            .build();

    private final @NotNull BlockSumoGame game;

    public InstaBreakEvent(@NotNull BlockSumoGame game) {
        this.game = game;
    }

    @Override
    public void start() {
        for (Player player : this.game.getPlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) continue;

            this.replaceShearsItem(player, ENCHANTED_SHEARS);
        }

        this.game.getInstance().scheduler().buildTask(this::end).delay(TaskSchedule.tick(ServerProperties.SERVER_TICKS_PER_SECOND.get() * 10)).schedule();
    }

    private void end() {
        for (Player player : this.game.getPlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) continue;

            this.replaceShearsItem(player, ItemStack.of(Material.SHEARS, 1)); // replace with default shears
        }
    }

    @Override
    public @NotNull Component getStartMessage() {
        return START_MESSAGE;
    }

    private void replaceShearsItem(@NotNull Player player, @NotNull ItemStack itemStack) {
        PlayerInventory inventory = player.getInventory();
        if (inventory.getCursorItem().material().equals(Material.SHEARS)) inventory.setCursorItem(itemStack);

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItemStack(i);

            if (stack.material().equals(Material.SHEARS)) {
                inventory.setItemStack(i, itemStack);
                return;
            }
        }
    }
}
