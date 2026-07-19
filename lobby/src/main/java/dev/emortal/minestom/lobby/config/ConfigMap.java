package dev.emortal.minestom.lobby.config;

import com.alibaba.fastjson2.annotation.JSONCompiled;
import org.jetbrains.annotations.NotNull;

@JSONCompiled
public record ConfigMap(@NotNull String id, boolean enabled, @NotNull String friendlyName, int priority, @NotNull ConfigItem displayItem) {
}