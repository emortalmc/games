package dev.emortal.messaging.message;

import com.alibaba.fastjson2.annotation.JSONCompiled;

@JSONCompiled
public record ProxyOnlineMessage() implements RedisMessage {
}
