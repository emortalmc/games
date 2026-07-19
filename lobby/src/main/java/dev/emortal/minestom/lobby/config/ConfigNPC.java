package dev.emortal.minestom.lobby.config;

import com.alibaba.fastjson2.annotation.JSONCompiled;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JSONCompiled
public record ConfigNPC(@NotNull String entityType, @NotNull List<String> titles, @NotNull ConfigSkin skin) {
}