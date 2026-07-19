package dev.emortal.messaging.types;

import com.alibaba.fastjson2.annotation.JSONCompiled;

@JSONCompiled
public record MarathonData(String time, String blockPalette, String animation) {
}
