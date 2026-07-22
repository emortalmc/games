package dev.emortal.minestom.core.map;

import com.alibaba.fastjson2.JSONObject;
import net.minestom.server.instance.InstanceContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record LoadedMap(@NotNull InstanceContainer instance, @Nullable JSONObject data) {
    }