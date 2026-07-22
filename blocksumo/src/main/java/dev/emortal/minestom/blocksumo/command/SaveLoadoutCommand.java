package dev.emortal.minestom.blocksumo.command;

import dev.emortal.messaging.types.BlockSumoData;
import dev.emortal.minestom.blocksumo.BlockSumoModule;
import dev.emortal.minestom.blocksumo.game.BlockSumoGame;
import dev.emortal.minestom.core.game.GameProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class SaveLoadoutCommand extends Command {

    public SaveLoadoutCommand(@NotNull GameProvider gameProvider) {
        super("saveloadout");

        this.setCondition(Conditions::playerOnly);
        this.setDefaultExecutor((sender, context) -> {
            Player player = (Player) sender;
            BlockSumoGame game = (BlockSumoGame) gameProvider.findGame(player);

            int shearsSlot = findShearsSlot(player);
            int blockSlot = findWoolSlot(player);

            if (shearsSlot == -1 || blockSlot == -1) {
                player.sendMessage(Component.text("You must be alive to save your loadout!", NamedTextColor.RED));
                return;
            }

            BlockSumoData playerData = game.getPlayerDataMap().getOrDefault(player.getUuid(), BlockSumoModule.DEFAULT_PLAYER_DATA);
            if (playerData.blockSlot() == blockSlot && playerData.shearsSlot() == shearsSlot) { // loadout hasn't changed
                player.sendMessage(Component.text("Loadout saved!", NamedTextColor.GREEN));
                return;
            }

            BlockSumoData newPlayerData = new BlockSumoData(shearsSlot, blockSlot);

            // Update data in the existing game
            game.getPlayerDataMap().put(player.getUuid(), newPlayerData);
            // Update data in the DB
            // TODO: this

            player.sendMessage(Component.text("Loadout saved!", NamedTextColor.GREEN));
        });
    }

    private int findShearsSlot(@NotNull Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (player.getInventory().getItemStack(i).material() == Material.SHEARS) {
                return i;
            }
        }

        return -1;
    }

    private int findWoolSlot(@NotNull Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (player.getInventory().getItemStack(i).material().name().toUpperCase(Locale.ROOT).endsWith("_WOOL")) {
                return i;
            }
        }

        return -1;
    }


}
