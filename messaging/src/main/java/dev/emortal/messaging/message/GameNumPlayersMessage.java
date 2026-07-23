package dev.emortal.messaging.message;

import com.alibaba.fastjson2.annotation.JSONCompiled;

import java.util.Map;
import java.util.UUID;

@JSONCompiled
public record GameNumPlayersMessage(UUID serverUUID, Map<String, Integer> online) implements RedisMessage {}
