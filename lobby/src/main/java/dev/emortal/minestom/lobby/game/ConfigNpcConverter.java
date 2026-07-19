package dev.emortal.minestom.lobby.game;

import dev.emortal.minestom.lobby.config.ConfigNPC;
import dev.emortal.minestom.lobby.config.ConfigSkin;
import dev.emortal.minestom.lobby.util.entity.BetterEntity;
import dev.emortal.minestom.lobby.util.entity.MultilineHologram;
import dev.emortal.minestom.lobby.util.npc.NpcHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.entity.metadata.avatar.MannequinMeta;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.network.player.ResolvableProfile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

final class ConfigNpcConverter {

    static @NotNull Entity convertNpc(@NotNull ConfigNPC npc, @NotNull Instance instance, @NotNull Pos pos,
                                      NpcHandler clickHandler) {

        ConfigSkin configSkin = npc.skin();
        PlayerSkin skin = new PlayerSkin(configSkin.texture(), configSkin.signature());

        BetterEntity emortalNpc = new BetterEntity(EntityType.MANNEQUIN);
        emortalNpc.setTicking(false);
        emortalNpc.editEntityMeta(MannequinMeta.class, meta -> {
            meta.setProfile(new ResolvableProfile(skin));
        });

//        EventNode<@NotNull Event> npcEventNode = EventNode.all("npc-interact." + emortalNpc.getEntityId());
//        MinecraftServer.getGlobalEventHandler().addChild(npcEventNode);
        var npcEventNode = instance.eventNode();

        npcEventNode.addListener(PlayerEntityInteractEvent.class, event -> {
            if (event.getHand() != PlayerHand.MAIN) return;
            Player player = event.getPlayer();
            if (event.getTarget() != emortalNpc) return;

            clickHandler.handlePlayerInteract(player, ClickType.RIGHT_CLICK);
        });
        npcEventNode.addListener(EntityAttackEvent.class, e -> {
            if (!(e.getEntity() instanceof Player player)) return;
            if (e.getTarget() != emortalNpc) return;

            clickHandler.handlePlayerInteract(player, ClickType.LEFT_CLICK);
        });

        emortalNpc.setInstance(instance, pos);

        return emortalNpc;
    }

    static @NotNull MultilineHologram convertHologram(@NotNull ConfigNPC npc, @NotNull Instance instance, @NotNull Pos pos) {
        List<Component> titles = convertTitles(npc.titles());
        titles.add(Component.text("0 playing", NamedTextColor.GRAY));

        Pos hologramPos = getHologramPos(pos);
        return new MultilineHologram(titles, instance, hologramPos);
    }

    private static @NotNull Pos getHologramPos(@NotNull Pos npcPos) {
        double height = (EntityType.PLAYER.height() + 0.2) / 2.0;
        return npcPos.add(0, height, 0);
    }

    private static @NotNull List<Component> convertTitles(@NotNull List<String> titles) {
        List<Component> result = new ArrayList<>();
        for (String title : titles) {
            result.add(MiniMessage.miniMessage().deserialize(title));
        }
        return result;
    }

    private ConfigNpcConverter() {
    }
}
