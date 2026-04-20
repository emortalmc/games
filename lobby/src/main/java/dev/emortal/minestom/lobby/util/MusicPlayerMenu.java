package dev.emortal.minestom.lobby.util;

import dev.emortal.minestom.core.utils.Menu;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.jukebox.JukeboxSong;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.registry.Registries;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@SuppressWarnings("PatternValidation")
public class MusicPlayerMenu extends Menu {
    private static final Tag<@NotNull String> PLAYING_DISC_TAG = Tag.Transient("playingDisc");

    public MusicPlayerMenu(Player player) {
        super(player, InventoryType.CHEST_6_ROW, "Music Discs");

        var i = 10;

        for (RegistryKey<@NotNull JukeboxSong> song : MinecraftServer.getJukeboxSongRegistry().keys()) {
            if ((i + 1) % 9 == 0) i += 2;
            this.set(i, itemFromJukeboxSong(song), _ -> this.startPlaying(song));
            i++;
        }

        ItemStack stopItem = ItemStack.of(Material.BARRIER)
                .with(DataComponents.ITEM_NAME, Component.text("Stop", NamedTextColor.RED, TextDecoration.BOLD));

        this.set(49, stopItem, _ -> this.startPlaying(null));
    }

    private void startPlaying(@Nullable RegistryKey<JukeboxSong> newSong) {
        String currentSong = this.player.getTag(PLAYING_DISC_TAG);

        if (currentSong != null) {
            this.player.stopSound(SoundStop.named(Key.key(currentSong)));
        }

        if (newSong != null) {
            JukeboxSong song = Objects.requireNonNull(MinecraftServer.getJukeboxSongRegistry().get(newSong));
            this.player.playSound(Sound.sound(song.soundEvent(), Sound.Source.RECORD, 1f, 1f), Sound.Emitter.self());
            this.player.setTag(PLAYING_DISC_TAG, song.soundEvent().name());
        }
    }

    private static ItemStack itemFromJukeboxSong(RegistryKey<@NotNull JukeboxSong> songKey) {
        Registries registries = MinecraftServer.process();
        JukeboxSong song = registries.jukeboxSong().get(songKey);
        if (song == null) return ItemStack.AIR;
        Material discMaterial = Material.fromKey(song.soundEvent().name().replace(".", "_"));
        if (discMaterial == null) return ItemStack.AIR;

        return ItemStack.builder(discMaterial)
                .set(DataComponents.JUKEBOX_PLAYABLE, songKey)
                .build();
    }
}