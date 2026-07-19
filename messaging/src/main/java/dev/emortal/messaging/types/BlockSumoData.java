package dev.emortal.messaging.types;

import com.alibaba.fastjson2.annotation.JSONCompiled;

@JSONCompiled
public record BlockSumoData(int shearsSlot, int blockSlot) {
}
