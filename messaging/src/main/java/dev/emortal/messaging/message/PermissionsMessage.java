package dev.emortal.messaging.message;

import com.alibaba.fastjson2.annotation.JSONCompiled;

import java.util.Collection;
import java.util.UUID;

@JSONCompiled
public record PermissionsMessage(UUID player, Collection<String> permissions) implements RedisMessage {
}
