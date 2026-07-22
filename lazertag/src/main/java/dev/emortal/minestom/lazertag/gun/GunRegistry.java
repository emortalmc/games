package dev.emortal.minestom.lazertag.gun;

import dev.emortal.minestom.lazertag.game.LazerTagGame;
import dev.emortal.minestom.lazertag.gun.guns.AssaultRifle;
import dev.emortal.minestom.lazertag.gun.guns.GunGun;
import dev.emortal.minestom.lazertag.gun.guns.Minigun;
import dev.emortal.minestom.lazertag.gun.guns.Shotgun;
import net.minestom.server.utils.WeightedList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class GunRegistry {

    private final @NotNull LazerTagGame game;
    private final @NotNull WeightedList<Gun> guns;

    private final Map<String, Gun> registry = new HashMap<>();

    public GunRegistry(@NotNull LazerTagGame game) {
        this.game = game;

        this.registerGuns();

        List<WeightedList.Entry<Gun>> weightedEntries = new ArrayList<>();
        for (Gun value : this.registry.values()) {
            weightedEntries.add(value.getWeightedEntry());
        }

        this.guns = new WeightedList<>(weightedEntries);
    }

    public @Nullable Gun getByName(@NotNull String name) {
        return this.registry.get(name);
    }

    public @NotNull Gun getRandomGun() {
        return this.guns.pickOrThrow(ThreadLocalRandom.current());
    }

    public void register(@NotNull Gun gun) {
        String name = gun.getName();
        if (this.registry.containsKey(name)) {
            throw new IllegalArgumentException("Gun with name " + name + " already exists!");
        }
        this.registry.put(name, gun);
    }

    public void registerGuns() {
        this.register(new AssaultRifle(this.game));
        this.register(new GunGun(this.game));
        this.register(new Minigun(this.game));
        this.register(new Shotgun(this.game));
//        this.register(new BeeBlaster(this.game));
//        this.register(new BeeMinigun(this.game));
//        this.register(new RBG(this.game));
//        this.register(new BlockChucker(this.game));
//        this.register(new Catipult(this.game));
    }
}
