package dev.emortal.messaging.message;

import com.alibaba.fastjson2.annotation.JSONCompiled;

import java.util.List;
import java.util.UUID;

@JSONCompiled
public record MatchmakeMessage(String gameId, List<UUID> players) implements RedisMessage {

}
