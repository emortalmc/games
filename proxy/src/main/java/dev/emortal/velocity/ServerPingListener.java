package dev.emortal.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

public class ServerPingListener {

    private static final Component MAIN_MOTD = Component.text()
            .append(Component.text("▓▒░              ", NamedTextColor.LIGHT_PURPLE))
            .append(Component.text("⚡   ", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
            .append(MiniMessage.miniMessage().deserialize("<gradient:gold:light_purple><bold>EmortalMC</bold></gradient>"))
            .append(Component.text("   ⚡", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("              ░▒▓", NamedTextColor.GOLD))
            .build();

    private static final String[] MOTDS = new String[] {
            "coolest server to ever exist",
            "better than hypixel",
            "you should join",
            "stop scrolling, click here!",
            "Lunar client users: Beware!",
            "gradient lover",
            "emortal is watching",
            "emortal says 2 + 2 = 5",
            "Chuck Norris joined and said it was pretty good",
            "Chuck Norris doesn't join, the server joins him",
            "private lobbies when?",
            "This server is certified aladeen!",
            "Bumboclat",
            "Also try Minehub!"
    };

    @Subscribe
    void onServerPing(@NotNull ProxyPingEvent event) {
        ServerPing ping;
        try {
            ping = event.getPing().asBuilder()
                    .description(createMessage())
                    .favicon(Favicon.create(Path.of("icon.png")))
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        event.setPing(ping);
    }

    private @NotNull Component createMessage() {
        String randomMessage = this.selectRandomMessage();
        return Component.text()
                .append(MAIN_MOTD)
                .appendNewline()
                .append(Component.text(randomMessage, NamedTextColor.YELLOW))
                .build();
    }

    private @NotNull String selectRandomMessage() {
        return MOTDS[ThreadLocalRandom.current().nextInt(MOTDS.length)];
    }

}