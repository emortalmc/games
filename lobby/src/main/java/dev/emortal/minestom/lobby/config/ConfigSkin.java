package dev.emortal.minestom.lobby.config;

import com.alibaba.fastjson2.annotation.JSONCompiled;
import org.jetbrains.annotations.NotNull;

@JSONCompiled
public record ConfigSkin(@NotNull String texture, @NotNull String signature) {
}