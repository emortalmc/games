package dev.emortal.minestom.core.map;

import com.alibaba.fastjson2.JSONObject;
import net.minestom.server.instance.InstanceContainer;
import org.jetbrains.annotations.NotNull;

public record LoadedMap(@NotNull InstanceContainer instance, @NotNull JSONObject data) {
    }