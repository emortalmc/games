package dev.emortal.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.emortal.messaging.message.Channel;
import dev.emortal.messaging.message.OnlinePlayersMessage;
import dev.emortal.messaging.types.GameInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerListener.class);

    private final CorePlugin plugin;
    private final ProxyServer proxy;
    public ServerListener(CorePlugin plugin, ProxyServer proxy) {
        this.plugin = plugin;
        this.proxy = proxy;
    }

    @Subscribe
    void chooseInitialServer(PlayerChooseInitialServerEvent event) {
        RegisteredServer lobby = plugin.getServer("lobby");
        if (lobby == null) {
            LOGGER.error("No lobby server for player to join");
            return;
        }

        event.setInitialServer(lobby);
    }

    @Subscribe
    void kicked(KickedFromServerEvent event) {
        RegisteredServer lobby = plugin.getServer("lobby");
        event.setResult(KickedFromServerEvent.RedirectPlayer.create(lobby, null));
    }

    @Subscribe
    void changeServer(ServerPostConnectEvent event) {
        updateOnlinePlayers();
    }
    @Subscribe
    void changeServer(DisconnectEvent event) {
        updateOnlinePlayers();
    }

    private void updateOnlinePlayers() {
        plugin.getRedis().sendMessage(Channel.ALL, new OnlinePlayersMessage(getOnlinePlayers()));

        proxy.sendPlayerListHeader(() -> createTablistHeader());
        proxy.sendPlayerListFooter(() -> createTablistFooter(proxy.getPlayerCount()));
    }

    private Component createTablistHeader() {
        return Component.text()
                .append(Component.text("┌                                                  ", NamedTextColor.GOLD))
                .append(Component.text("┐ ", NamedTextColor.LIGHT_PURPLE))
                .appendNewline()
                .append(MiniMessage.miniMessage().deserialize("<gradient:gold:light_purple><bold>EmortalMC</bold></gradient>"))
                .appendNewline()
                .build();
    }

    private Component createTablistFooter(int online) {
        return Component.text()
                .append(Component.text(" ", NamedTextColor.GRAY)).appendNewline()
                .append(Component.text(online + " online", NamedTextColor.GRAY)).appendNewline()
                .append(Component.text("ᴍᴄ.ᴇᴍᴏʀᴛᴀʟ.ᴅᴇᴠ", TextColor.color(0x266ee0))).appendNewline()
                .append(Component.text("└                                                  ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text("┘ ", NamedTextColor.GOLD))
                .build();
    }

    public Map<String, Integer> getOnlinePlayers() {
        Map<String, Integer> onlinePlayers = new HashMap<>();

        for (Map.Entry<UUID, Collection<GameInfo>> entry : plugin.getSupportedGamesMap().entrySet()) {
            RegisteredServer server = proxy.getServer(entry.getKey().toString()).orElse(null);
            if (server == null) continue;
            int numOnline = server.getPlayersConnected().size();

            for (GameInfo s : entry.getValue()) {
                onlinePlayers.compute(s.gameId(), (_, b) -> (b == null ? 0 : b) + numOnline); // add to map
            }
        }

        return onlinePlayers;
    }

}
