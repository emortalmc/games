package dev.emortal.minestom.lobby.config;

import com.alibaba.fastjson2.annotation.JSONCompiled;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@JSONCompiled
public record GameModeConfig(@NotNull String id, boolean enabled, @NotNull String fleetName, int priority,
                             @NotNull String friendlyName, @NotNull String activityNoun,
                             @Nullable ConfigItem displayItem, @Nullable ConfigNPC displayNpc,
                             @Nullable Map<String, ConfigMap> maps) {}