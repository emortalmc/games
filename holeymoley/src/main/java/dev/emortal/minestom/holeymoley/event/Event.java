package dev.emortal.minestom.holeymoley.event;

import dev.emortal.minestom.holeymoley.game.HoleyMoleyGame;

public abstract class Event {

    private final String name;

    public Event(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract void doEvent(HoleyMoleyGame game);

}
