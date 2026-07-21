package dev.emortal.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.emortal.messaging.message.PermissionsMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.query.QueryOptions;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PermissionsListener {

    private final CorePlugin plugin;
    public PermissionsListener(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPreConnect(ServerPreConnectEvent event) {
        RegisteredServer server = event.getResult().getServer().orElse(null);
        if (server == null) return;
        String uuidString = server.getServerInfo().getName();
        UUID uuid = UUID.fromString(uuidString);

        Set<String> permissions = getPlayerPermissions(event.getPlayer());
        plugin.getRedis().sendServerMessage(uuid, new PermissionsMessage(event.getPlayer().getUniqueId(), permissions)).join();
    }

    private Set<String> getPlayerPermissions(Player player) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return new HashSet<>();
        Set<String> permissions = new HashSet<>();
        for (Node node : user.resolveInheritedNodes(QueryOptions.nonContextual())) {
            if (!node.getValue()) continue; // ignore false permissions
            permissions.add(node.getKey());
        }
        return permissions;
    }

}
