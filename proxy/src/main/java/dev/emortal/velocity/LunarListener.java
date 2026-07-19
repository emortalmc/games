package dev.emortal.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerClientBrandEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

final class LunarListener {
    private static final Component LUNAR_MESSAGE = Component.text()
            .append(Component.text("Please consider using a more capable client, such as Fabric. Lunar causes many random issues.", NamedTextColor.RED))
            .build();

    @Subscribe
    void onPlayerJoin(@NotNull PlayerClientBrandEvent event) {
        Player player = event.getPlayer();
        String brand = event.getBrand().toLowerCase();

        if (brand.contains("lunar")) {
            player.sendMessage(LUNAR_MESSAGE);
        }
    }
}
