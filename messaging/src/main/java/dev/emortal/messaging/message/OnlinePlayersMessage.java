package dev.emortal.messaging.message;

import com.alibaba.fastjson2.annotation.JSONCompiled;

import java.util.Map;

@JSONCompiled
public record OnlinePlayersMessage(Map<String, Integer> online) implements RedisMessage {}
