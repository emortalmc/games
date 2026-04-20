package dev.emortal.minestom.lobby.emote;

import dev.emortal.minestom.core.utils.Menu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class EmoteMenu extends Menu {
    public EmoteMenu(Player player) {
        super(player, InventoryType.CHEST_1_ROW, "Emotes");

        int slotI = 0;

        for (Emote.Type emote : Emote.Type.values()) {
            Component itemName = Component.text(emote.getFriendlyName(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false);

            this.set(slotI, ItemStack.of(Material.DIAMOND).with(DataComponents.ITEM_NAME, itemName), _ -> {
                Emote.stop(player);

                player.scheduleNextTick(_ -> {
                    Emote.play(player, emote);
                    player.setHeldItemSlot((byte) 2);
                });

                player.closeInventory();
            });

            slotI++;
        }

        ItemStack stopItem = ItemStack.of(Material.BARRIER)
                .with(DataComponents.ITEM_NAME, Component.text("Stop", NamedTextColor.RED));

        this.set(8, stopItem, _ -> Emote.stop(this.player));
    }
}
