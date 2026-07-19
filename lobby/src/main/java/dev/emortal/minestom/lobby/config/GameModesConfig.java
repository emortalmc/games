package dev.emortal.minestom.lobby.config;

import com.alibaba.fastjson2.annotation.JSONCompiled;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JSONCompiled
public record GameModesConfig(@NotNull List<GameModeConfig> gamemodes) {}