package dev.emortal.minestom.lobby;

import dev.emortal.minestom.core.utils.command.ExtraConditions;
import dev.emortal.minestom.lobby.emote.Emote;
import dev.emortal.minestom.lobby.gadget.Fireball;
import dev.emortal.minestom.lobby.gadget.Gadget;
import dev.emortal.minestom.lobby.gadget.LightningRod;
import dev.emortal.minestom.lobby.gadget.Trumpet;
import dev.emortal.minestom.lobby.gadget.blaster.InkBlaster;
import dev.emortal.minestom.lobby.gadget.lobber.BlockLobber;
import dev.emortal.minestom.lobby.util.CustomModels;
import dev.emortal.minestom.lobby.util.MusicPlayerInventory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class LobbyEvents {
    public static final Tag<@NotNull Boolean> SERVER_SELECTOR_TAG = Tag.Boolean("serverSelector");
    public static final ItemStack SERVER_SELECTOR_ITEM = ItemStack.builder(Material.COMPASS)
            .set(DataComponents.ITEM_NAME, Component.text("Server Selector", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
            .set(SERVER_SELECTOR_TAG, true)
            .build();
    public static final Tag<@NotNull Boolean> MUSIC_PLAYER_TAG = Tag.Boolean("musicPlayer");
    public static final ItemStack MUSIC_PLAYER_ITEM = ItemStack.builder(Material.JUKEBOX)
            .set(DataComponents.ITEM_NAME, Component.text("Music Player", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false))
            .set(MUSIC_PLAYER_TAG, true)
            .build();
    public static final Tag<@NotNull Boolean> EMOTES_TAG = Tag.Boolean("emotes");
    public static final ItemStack EMOTES_ITEM = ItemStack.builder(Material.PHANTOM_MEMBRANE)
            .itemModel(CustomModels.EMOTES.getModelId())
            .set(DataComponents.ITEM_NAME, Component.text("Emotes", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
            .set(EMOTES_TAG, true)
            .build();

    private static final String EMORTAL_UUID = "7bd5b459-1e6b-4753-8274-1fbd2fe9a4d5";

    private static final List<Gadget> GADGETS = List.of(
            new Fireball(),
            new BlockLobber(),
            new Trumpet(),
            new LightningRod(),
            new InkBlaster()
//            new BubbleBlower()
    );

    public static void registerGeneric(@NotNull EventNode<@NotNull Event> eventNode, @NotNull Instance instance) {
        eventNode.addListener(PlayerSpawnEvent.class, event -> onSpawn(event.getPlayer(), event.getInstance(), instance));
        eventNode.addListener(PlayerUseItemEvent.class, LobbyEvents::onItemUse);

        for (Gadget gadget : GADGETS) {
            gadget.registerListeners(eventNode);
        }
    }

    public static void onSpawn(@NotNull Player player, @NotNull Instance spawnInstance, @NotNull Instance instance) {
        if (spawnInstance != instance) return;

        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);

        player.getInventory().setItemStack(4, SERVER_SELECTOR_ITEM);
        player.getInventory().setItemStack(0, MUSIC_PLAYER_ITEM);

        if (ExtraConditions.hasPermission(player, "lobby.emotes")) {
            player.getInventory().setItemStack(1, EMOTES_ITEM);
        }

        int gadgetSlot = 9;
        for (Gadget gadget : GADGETS) {
            if (!gadget.isAllowed(player)) continue;
            gadget.give(player, gadgetSlot);
            gadgetSlot++;
        }

        handlePlayerSpecific(player);

        if (ExtraConditions.hasPermission(player, "lobby.fly")) player.setAllowFlying(true);
        player.setTag(LobbyTags.LOBBABLE, true);
    }

    private static void onItemUse(@NotNull PlayerUseItemEvent event) {
        if (event.getHand() != PlayerHand.MAIN) return;

        Player player = event.getPlayer();
        ItemStack mainHandItem = player.getItemInMainHand();
        if (mainHandItem.hasTag(MUSIC_PLAYER_TAG)) {
            cancel(event);
            player.openInventory(MusicPlayerInventory.getInventory());
        }

        if (mainHandItem.hasTag(EMOTES_TAG)) {
            cancel(event);
            Emote.openInventory(player);
        }



    }

    public static void registerProtectionEvents(@NotNull EventNode<@NotNull Event> eventNode, @NotNull Instance spawnInstance) {
        eventNode.addListener(AsyncPlayerConfigurationEvent.class, event -> onLogin(event, spawnInstance));
        eventNode.addListener(PlayerMoveEvent.class, LobbyEvents::onMove);

        eventNode.addListener(ItemDropEvent.class, LobbyEvents::cancel)
                .addListener(PlayerSwapItemEvent.class, LobbyEvents::cancel)
                .addListener(PlayerBlockBreakEvent.class, LobbyEvents::cancel)
                .addListener(PlayerBlockPlaceEvent.class, LobbyEvents::cancel);
    }

    private static void onLogin(@NotNull AsyncPlayerConfigurationEvent event, @NotNull Instance spawnInstance) {
        event.setSpawningInstance(spawnInstance);
        event.getPlayer().setRespawnPoint(Entrypoint.SPAWN_POINT);
    }

    private static void onMove(@NotNull PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getPosition().y() < -10) {
            player.teleport(Entrypoint.SPAWN_POINT);
        }
    }

    private static void handlePlayerSpecific(@NotNull Player player) {
        switch (player.getUuid().toString()) {
            case EMORTAL_UUID -> {
                player.setHelmet(
                        ItemStack.builder(Material.PHANTOM_MEMBRANE)
                                .itemModel(CustomModels.EMORTAL_BIRTHDAY_HAT.getModelId())
                                .build()
                );
            }
        }
    }

    private static void cancel(@NotNull CancellableEvent event) {
        event.setCancelled(true);
    }

    private LobbyEvents() {
    }
}
