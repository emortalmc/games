package dev.emortal.minestom.core;

import dev.emortal.messaging.RedisMessenger;
import dev.emortal.messaging.message.PermissionsMessage;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PermissionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionHandler.class);

    private final Map<UUID, Set<String>> permissionMap = new HashMap<>();
    public PermissionHandler(RedisMessenger redis, EventNode<Event> eventNode) {
        redis.addMessageHandler(PermissionsMessage.class, (channel, msg) -> {
            HashSet<String> permissionSet = new HashSet<>(msg.permissions());
            permissionMap.put(msg.player(), permissionSet);
            LOGGER.info("Received {} permissions for player {}", msg.permissions().size(), msg.player());
        });

        eventNode.addListener(PlayerDisconnectEvent.class, e -> {
            permissionMap.remove(e.getPlayer().getUuid());
        });
    }

    public boolean hasPermission(Player player, String permission) {
        Set<String> permissionSet = permissionMap.get(player.getUuid());
        if (permissionSet == null) return false;
        return hasPermission(permissionSet, permission);
    }

    private boolean hasPermission(Set<String> permissionSet, String permission) {
        if (permissionSet.contains(permission)) return true;
        if (permissionSet.contains("*")) return true;

        for (String s : permissionSet) {
            boolean contains = hasWildcards(permissionSet, s);
            if (contains) return true;
        }

        return false;
    }

    private boolean hasWildcards(Set<String> permissionSet, String permission) {
        while (permission != null) {
            permission = getWildcard(permission);
            boolean contains = permissionSet.contains(permission);
            if (contains) return true;
        }
        return false;
    }

    private static String getWildcard(String str) {
        if (str.equals("*")) return null;
        List<String> strings = splitNonRegex(str, ".");
        if (strings.getLast().equalsIgnoreCase("*")) strings.removeLast();
        strings.removeLast();
        if (strings.isEmpty()) return "*";
        return String.join(".", strings) + ".*";
    }

    private static List<String> splitNonRegex(String input, String delim) {
        List<String> l = new ArrayList<>();
        int offset = 0;

        while (true) {
            int index = input.indexOf(delim, offset);
            if (index == -1) {
                l.add(input.substring(offset));
                return l;
            } else {
                l.add(input.substring(offset, index));
                offset = (index + delim.length());
            }
        }
    }

}
