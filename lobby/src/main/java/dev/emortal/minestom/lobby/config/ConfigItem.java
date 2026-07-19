package dev.emortal.minestom.lobby.config;

import com.alibaba.fastjson2.annotation.JSONCompiled;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JSONCompiled
public record ConfigItem(@NotNull String material, int slot, @NotNull String name, @NotNull List<String> lore) {
}