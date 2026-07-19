package dev.emortal.messaging.types;

import com.alibaba.fastjson2.annotation.JSONCompiled;

import java.util.Collection;

@JSONCompiled
public record GameInfo(String gameId, Collection<String> maps, int minPlayers, int maxPlayers, MatchMethod matchMethod) {

    public enum MatchMethod {
        INSTANT, COUNTDOWN
    }

}
