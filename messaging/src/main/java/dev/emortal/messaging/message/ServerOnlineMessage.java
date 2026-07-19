package dev.emortal.messaging.message;

import com.alibaba.fastjson2.annotation.JSONCompiled;
import dev.emortal.messaging.types.GameInfo;

import java.util.Collection;
import java.util.UUID;

@JSONCompiled
public record ServerOnlineMessage(UUID serverId, String address, int port, Collection<GameInfo> games) implements RedisMessage {
}
