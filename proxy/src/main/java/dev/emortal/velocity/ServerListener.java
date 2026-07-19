package dev.emortal.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.emortal.messaging.message.Channel;
import dev.emortal.messaging.message.OnlinePlayersMessage;
import dev.emortal.messaging.types.GameInfo;
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
    void changeServer(ServerPostConnectEvent event) {
        plugin.getRedis().sendMessage(Channel.ALL, new OnlinePlayersMessage(getOnlinePlayers()));
    }
    @Subscribe
    void changeServer(DisconnectEvent event) {
        plugin.getRedis().sendMessage(Channel.ALL, new OnlinePlayersMessage(getOnlinePlayers()));
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
