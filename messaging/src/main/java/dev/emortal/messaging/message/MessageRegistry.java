package dev.emortal.messaging.message;

import com.alibaba.fastjson2.JSON;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageRegistry {

    private static final Map<String, Class<? extends RedisMessage>> TYPES = new HashMap<>() {{
        put("proxy_online", ProxyOnlineMessage.class);
        put("server_online", ServerOnlineMessage.class);
        put("create_game", CreateGameMessage.class);
        put("game_ready", GameReadyMessage.class);
        put("send_lobby", SendLobbyMessage.class);
        put("online_players", OnlinePlayersMessage.class);
        put("matchmake", MatchmakeMessage.class);
        put("map_vote", MapVoteMessage.class);
        put("permissions", PermissionsMessage.class);
    }};

    private static final Map<Class<?>, String> IDS = TYPES.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    public static String encode(RedisMessage message) {
        String id = IDS.get(message.getClass());
        if (id == null) throw new IllegalArgumentException("Unregistered type: " + message.getClass());
        return id + " " + JSON.toJSONString(message);
    }

    @SuppressWarnings("unchecked")
    public static <T extends RedisMessage> T decode(String message) {
        int idx = message.indexOf(" ");
        if (idx < 0) throw new IllegalArgumentException("Malformed message: " + message);
        String id = message.substring(0, idx);
        String jsonString = message.substring(idx + 1);
        Class<? extends RedisMessage> clazz = TYPES.get(id);
        if (clazz == null) throw new IllegalArgumentException("Unknown type: " + id);
        return (T) JSON.parseObject(jsonString, clazz);
    }

}
